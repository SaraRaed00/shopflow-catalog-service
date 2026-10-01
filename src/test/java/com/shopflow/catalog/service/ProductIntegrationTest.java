package com.shopflow.catalog.service;

import com.shopflow.catalog.domain.exception.ConflictException;
import com.shopflow.catalog.domain.exception.NotFoundException;
import com.shopflow.catalog.domain.model.*;
import com.shopflow.catalog.repository.*;
import com.shopflow.catalog.support.AbstractIntegrationTest;
import com.shopflow.catalog.support.ProductFixtures;
import com.shopflow.catalog.web.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@AutoConfigureTestRestTemplate
public class ProductIntegrationTest extends AbstractIntegrationTest{
    @Autowired private ProductService productService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private StockItemRepository stockItemRepository;
    @Autowired private ReservationService reservationService;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ProductImportService productImportService;
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private OutboxPublisherJob outboxPublisherJob;

    // create operation sends correct data
    public void create_sends_correct_data(){
        Category category = new Category();
        category.setName("Books");
        category.setSlug("TEST-BOOKS");
        Category savedCategory = categoryRepository.save(category);

        CreateProductRequest request = new CreateProductRequest("TEST", "Test_Book","description", savedCategory.getId(),new BigDecimal("3.9"), "KWD");

        ProductResponse response = productService.create(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.sku()).isEqualTo("TEST");
        assertThat(response.createdAt()).isNotNull();
    }

    public void create_sku_duplicate_exception(){
        Category category = new Category();
        category.setName("Shoes");
        category.setSlug("TEST");
        Category savedCategory = categoryRepository.save(category);

        CreateProductRequest request = new CreateProductRequest("DUPLICATE", "Test_Shoes","description", savedCategory.getId(),new BigDecimal("15.9"), "KWD");

        ProductResponse response = productService.create(request);

        assertThatThrownBy(()-> productService.create(request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void update_shouldEvictCache_soSubsequentReadIsFresh() {
        // create + cache a product, update it, confirm the fresh value comes back
        Category category = new Category();
        category.setName("Cache Test");
        category.setSlug("cache-test-00001" );
        Category savedCategory = categoryRepository.save(category);

        CreateProductRequest request = new CreateProductRequest(
            "CACHE-TEST-01", "Original Name", "desc",
            savedCategory.getId(), new BigDecimal("10.000"), "KWD");
        ProductResponse created = productService.create(request);

        productService.findById(created.id()); // primes the cache

        UpdateProductRequest updateRequest = new UpdateProductRequest(
            "Updated Name", "desc", savedCategory.getId(),
            new BigDecimal("20.000"), "KWD");
        productService.update(created.id(), updateRequest); // evict cache

        ProductResponse afterUpdate = productService.findById(created.id()); // trigger cach
        assertThat(afterUpdate.name()).isEqualTo("Updated Name");
    }
    @Test
    void reservation_concurrency_test() throws InterruptedException{
        // category, product, warehouse, stockitem
        Category savedCategory = categoryRepository.save(ProductFixtures.aCategory().build());

        Product savedProduct = productRepository.save(ProductFixtures.aProduct().withCategory(savedCategory).build());

        Warehouse savedWarehouse = warehouseRepository.save(ProductFixtures.aWarehouse().build());

        StockItem savedStockItem = stockItemRepository.save(
            ProductFixtures.aStockItem()
                .withProduct(savedProduct)
                .withWarehouse(savedWarehouse)
                .withQuantity(5)
                .withReservedQty(0)
                .build());

        int ThreadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(ThreadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger exhaustedRetriesCount = new AtomicInteger(0);


        for(int i=0; i<20 ;i++){
            pool.submit(()-> {
                try {
                    startLatch.await();
                    CreateReservationRequest request = new CreateReservationRequest(savedStockItem.getProduct().getId(), savedStockItem.getWarehouse().getId(), 1);
                    ReservationResponse response = reservationService.create(request);
                    successCount.incrementAndGet();
                }
                catch (ConflictException exception){
                    conflictCount.incrementAndGet();
                }
                catch (org.springframework.dao.OptimisticLockingFailureException exception) {
                    exhaustedRetriesCount.incrementAndGet();
                }
                catch (InterruptedException exception){
                    Thread.currentThread().interrupt();
                }
            });
        }
        startLatch.countDown();

        pool.shutdown(); // stop accepting extra tasks
        boolean finished = pool.awaitTermination(10, TimeUnit.SECONDS); // stop the main thread until all 20 finish, but continue after 10s if a thread stucks

        assertThat(successCount.get()).isEqualTo(5);
        assertThat(conflictCount.get() + exhaustedRetriesCount.get()).isEqualTo(15);

        StockItem finalStock = stockItemRepository.findById(savedStockItem.getId()).orElseThrow();
        assertThat(finalStock.getReservedQty()).isEqualTo(5);

    }

    @Test // check that confirming reservation many times dont deduct extra quantity
    void confirm_isIdempotent(){
        Category savedCategory = categoryRepository.save(ProductFixtures.aCategory().build());

        Product savedProduct = productRepository.save(ProductFixtures.aProduct().withCategory(savedCategory).build());

        Warehouse savedWarehouse = warehouseRepository.save(ProductFixtures.aWarehouse().build());

        StockItem savedStockItem = stockItemRepository.save(
            ProductFixtures.aStockItem()
                .withProduct(savedProduct)
                .withWarehouse(savedWarehouse)
                .withQuantity(6)
                .withReservedQty(0)
                .build());

        CreateReservationRequest request = new CreateReservationRequest(savedProduct.getId(),savedWarehouse.getId(),2);
        ReservationResponse response = reservationService.create(request);

        reservationService.confirm(response.reference());
        reservationService.confirm(response.reference()); // call again

        StockItem checkStock = stockItemRepository.findById(savedStockItem.getId()).orElseThrow();

        assertThat(checkStock.getReservedQty()).isEqualTo(0);
        assertThat(checkStock.getQuantity()).isEqualTo(4);

    }

    @Test
    void create_shouldPersistNothing_whenStockItemMissing() {
        long reservationCountBefore = reservationRepository.count();

        CreateReservationRequest request = new CreateReservationRequest(999999L, 999999L, 1);

        assertThatThrownBy(() -> reservationService.create(request))
            .isInstanceOf(NotFoundException.class);

        assertThat(reservationRepository.count()).isEqualTo(reservationCountBefore);
    }

    @Test
    void release_isIdempotent(){
            Category category = new Category();
            category.setName("Release Idempotency Test");
            category.setSlug("release-idemp");
            Category savedCategory = categoryRepository.save(category);

            Product product = new Product();
            product.setSku("RELEASE-IDEMPOTENT");
            product.setName("Release Idempotency Product");
            product.setCategory(savedCategory);
            product.setPrice(new Money(new BigDecimal("5.000"), "KWD"));
            Product savedProduct = productRepository.save(product);

            Warehouse warehouse = new Warehouse();
            warehouse.setCode("REL-IDEMP");
            warehouse.setName("Release Idempotency Warehouse");
            warehouse.setCountry("KW");
            Warehouse savedWarehouse = warehouseRepository.save(warehouse);

            StockItem stockItem = new StockItem();
            stockItem.setProduct(savedProduct);
            stockItem.setWarehouse(savedWarehouse);
            stockItem.setQuantity(10);
            stockItem.setReservedQty(0);
            stockItemRepository.save(stockItem);

            CreateReservationRequest request = new CreateReservationRequest(
                savedProduct.getId(), savedWarehouse.getId(), 4);
            ReservationResponse reservation = reservationService.create(request);

            reservationService.release(reservation.reference());
            reservationService.release(reservation.reference()); // called twice, deliberately

            StockItem finalStock = stockItemRepository.findById(stockItem.getId()).orElseThrow();
            assertThat(finalStock.getQuantity()).isEqualTo(10);
            assertThat(finalStock.getReservedQty()).isEqualTo(0);
        }

    @Test
    void feed_shouldPutAllProductsInOrder_usingCursor() {
        Category category = categoryRepository.save(ProductFixtures.aCategory().build());
        for (int i = 0; i < 5; i++) {
            productRepository.save(ProductFixtures.aProduct().withCategory(category).build());
        }
        ProductSearchCriteria criteria = new ProductSearchCriteria(null, category.getId(), null, null, null);

        keysetPageResponse<ProductResponse> page1 = productService.feed(criteria, null, 2);
        assertThat(page1.content()).hasSize(2);
        assertThat(page1.hasNext()).isTrue();

        keysetPageResponse<ProductResponse> page2 = productService.feed(criteria, page1.nextCursor(), 2);
        assertThat(page2.content()).hasSize(2);
        assertThat(page2.hasNext()).isTrue();

        keysetPageResponse<ProductResponse> page3 = productService.feed(criteria, page2.nextCursor(), 2);
        assertThat(page3.content()).hasSize(1);
        assertThat(page3.hasNext()).isFalse();
        assertThat(page3.nextCursor()).isNull();

        List<Long> allIds = Stream.of(page1, page2, page3)
            .flatMap(p -> p.content().stream())
            .map(ProductResponse::id)
            .toList();
        assertThat(allIds).doesNotHaveDuplicates().isSorted();
    }

    @Test
    void importCsvFile_shouldImportGoodRows_andReportEachBadRow() throws IOException {
        // make the test repeatable: remove rows a previous run may have created
        productRepository.findBySku("CSV-IMP-1").ifPresent(productRepository::delete);
        productRepository.findBySku("CSV-IMP-6").ifPresent(productRepository::delete);

        ImportReport report;
        try (InputStream in = new ClassPathResource("import/products-mixed.csv").getInputStream()) {
            report = productImportService.importCsv(in);
        }

        for (ImportRowError error : report.errors()) {
            System.out.println("row " + error.row() + " [" + error.code() + "]: " + error.message());
        }

        assertThat(report.totalRows()).isEqualTo(7);
        assertThat(report.imported()).isEqualTo(2);
        assertThat(report.failedInvalidRowException()).isEqualTo(3);
        assertThat(report.failedNotFoundException()).isEqualTo(1);
        assertThat(report.failedDataIntegrity()).isEqualTo(0);
        assertThat(report.failedConflictException()).isEqualTo(1);

        assertThat(report.errors()).extracting(ImportRowError::row)
            .containsExactly(2L, 3L, 4L, 5L, 6L);
        assertThat(productRepository.existsBySku("CSV-IMP-1")).isTrue();
        assertThat(productRepository.existsBySku("CSV-IMP-6")).isTrue();
        assertThat(productRepository.existsBySku("CSV-IMP-2")).isFalse();
    }

    @Test
    void reservationCreate_shouldReturn429_afterTenRequestsInOneMinute() {
        Category category = categoryRepository.save(ProductFixtures.aCategory().build());
        Product product = productRepository.save(ProductFixtures.aProduct().withCategory(category).build());
        Warehouse warehouse = warehouseRepository.save(ProductFixtures.aWarehouse().build());
        stockItemRepository.save(ProductFixtures.aStockItem()
            .withProduct(product).withWarehouse(warehouse).withQuantity(100).build());

        CreateReservationRequest request = new CreateReservationRequest(product.getId(), warehouse.getId(), 1);

        for (int i = 0; i < 10; i++) {
            ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/reservations", request, String.class);
            assertThat(response.getStatusCode().value()).isNotEqualTo(429);
        }

        ResponseEntity<String> extra = restTemplate.postForEntity(
            "/api/v1/reservations", request, String.class);

        assertThat(extra.getStatusCode().value()).isEqualTo(429);
    }

    @Test
    void confirm_shouldWriteOutboxEvent_inSameTransaction() {
        Category category = categoryRepository.save(ProductFixtures.aCategory().build());
        Product product = productRepository.save(ProductFixtures.aProduct().withCategory(category).build());
        Warehouse warehouse = warehouseRepository.save(ProductFixtures.aWarehouse().build());
        stockItemRepository.save(ProductFixtures.aStockItem()
            .withProduct(product).withWarehouse(warehouse).withQuantity(10).build());

        ReservationResponse reservation = reservationService.create(
            new CreateReservationRequest(product.getId(), warehouse.getId(), 2));

        long before = outboxEventRepository.count();
        reservationService.confirm(reservation.reference());
        long after = outboxEventRepository.count();

        assertThat(after).isEqualTo(before + 1);

        OutboxEvent event = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
            "PENDING", PageRequest.of(0, 1)).get(0); // status should convert to published
        assertThat(event.getEventType()).isEqualTo("ReservationConfirmed");
        assertThat(event.getAggregateId()).isEqualTo(reservation.reference());
        assertThat(event.getPayload()).contains(reservation.reference());
    }

    @Test
    void failedConfirm_shouldWriteNoOutboxEvent() {
        long before = outboxEventRepository.count();

        assertThatThrownBy(() -> reservationService.confirm("does-not-exist"))
            .isInstanceOf(NotFoundException.class);

        assertThat(outboxEventRepository.count()).isEqualTo(before);
    }

    @Test
    void outboxPublisherJob_shouldMarkPendingEventsAsPublished() {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("Reservation");
        event.setAggregateId("ref-test");
        event.setEventType("ReservationConfirmed");
        event.setPayload("{}");
        event.setCreatedAt(Instant.now());
        outboxEventRepository.save(event);

        outboxPublisherJob.publishPendingEvents();

        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("PUBLISHED");
        assertThat(updated.getProcessedAt()).isNotNull();
    }

    }
