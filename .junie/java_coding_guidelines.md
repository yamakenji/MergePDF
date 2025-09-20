# Perfect Java Coding Conventions for AI Code Generation

## 🎯 目的と適用範囲

このドキュメントは、AIによるJavaコード生成において、**保守性・拡張性・テスト容易性・堅牢性**を兼ね備えた最高品質のコードを生成するための厳格な規約です。

---

## 📋 基本前提・制約

### 言語・バージョン
- **Java 21 LTS** を前提（モダン機能を積極活用）
- 外部ライブラリは**明示指示時のみ**使用（デフォルトは標準JDKのみ）

### 出力形式
- **「コードのみ」指示** → コードブロックのみ出力
- **通常指示** → 要約 → コード → ビルド/実行手順
- **前提・制約がある場合** → "Assumptions" セクションで明記

### コードフォーマット
- **Google Java Style準拠**（行長100、インデント4スペース、タブ禁止）
- 未使用import禁止、ワイルドカードimport禁止
- `@Override`は必須付与

---

## 🏗️ 核となる設計思想

### 1. Clean Code原則
```java
// ❌ 悪い例: 意図が不明
public void process(List<String> data, boolean flag) { /* ... */ }

// ✅ 良い例: 意図が明確
public void validateAndSaveCustomerData(List<CustomerRecord> records) { /* ... */ }
public void validateCustomerData(List<CustomerRecord> records) { /* ... */ }
```

### 2. SOLID原則の厳格適用
- **S**: 1クラス・1メソッド = 1責任（分割の目安：理由が複数ある場合）
- **O**: 拡張に開放、修正に閉鎖（インターフェース・抽象クラス活用）
- **L**: 派生クラスは基底クラスと完全置換可能
- **I**: 使用しないメソッドへの依存強制禁止
- **D**: 具象でなく抽象（インターフェース）に依存

### 3. 関数型プログラミング要素
```java
// ✅ 不変性優先
public record User(String name, int age, String email) {
    public User {
        Objects.requireNonNull(name, "Name cannot be null");
        if (age < 0) throw new IllegalArgumentException("Age must be positive");
    }
}

// ✅ 副作用分離（CQS原則）
public Optional<User> findUser(String id) { /* クエリ */ }
public void saveUser(User user) { /* コマンド */ }

// ✅ Stream APIで宣言的プログラミング
users.stream()
    .filter(User::isActive)
    .map(user -> user.name().toUpperCase())
    .collect(toList());
```

---

## 🔧 具体的コーディング規約

### 命名規則
```java
// クラス・インターフェース・Enum: UpperCamelCase
public class CustomerService { }
public interface PaymentProcessor { }
public enum OrderStatus { }

// メソッド・変数・パラメータ: lowerCamelCase  
public void calculateTotalAmount() { }
private final String customerName;

// 定数: UPPER_SNAKE_CASE
private static final int MAX_RETRY_COUNT = 3;

// パッケージ: 小文字のみ
package com.company.project.domain;
```

### クラス設計
```java
// ✅ 依存性注入（コンストラクタインジェクション）
public class OrderService {
    private final OrderRepository repository;
    private final PaymentService paymentService;
    
    public OrderService(OrderRepository repository, PaymentService paymentService) {
        this.repository = Objects.requireNonNull(repository);
        this.paymentService = Objects.requireNonNull(paymentService);
    }
}

// ✅ 継承よりコンポジション
public class PaymentProcessor {
    private final CreditCardValidator validator;
    private final PaymentGateway gateway;
    // コンストラクタ...
}
```

### メソッド設計
```java
// ✅ 短く簡潔（目安：10-15行以内）
public Optional<Customer> findActiveCustomer(String customerId) {
    return customerRepository.findById(customerId)
            .filter(Customer::isActive);
}

// ✅ 引数は0-2個（3個以上はパラメータオブジェクト）
public record CreateOrderRequest(
    String customerId,
    List<OrderItem> items,
    String deliveryAddress
) {}

public Order createOrder(CreateOrderRequest request) { /* ... */ }

// ❌ booleanフラグ引数は禁止
// public void processOrder(Order order, boolean isUrgent) { }

// ✅ 意図別メソッドに分割
public void processUrgentOrder(Order order) { /* ... */ }
public void processRegularOrder(Order order) { /* ... */ }
```

### エラーハンドリング
```java
// ✅ 具体的例外・cause保持・リソース管理
public List<String> readConfigFile(Path configPath) {
    try (var reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
        return reader.lines().collect(toList());
    } catch (IOException e) {
        throw new ConfigurationException("Failed to read config: " + configPath, e);
    }
}

// ✅ Optional活用（null禁止）
public Optional<User> findUserByEmail(String email) {
    return userRepository.findByEmail(email);
}

// ✅ 前提条件チェック
public void updateUserAge(String userId, int age) {
    Objects.requireNonNull(userId, "User ID cannot be null");
    if (age < 0) {
        throw new IllegalArgumentException("Age must be non-negative: " + age);
    }
    // 実装...
}
```

### Optional使用規則
```java
// ✅ 戻り値のみで使用
public Optional<Order> findOrder(String orderId) { /* ... */ }

// ✅ 安全な値取得
findOrder("123")
    .filter(order -> order.getStatus() == ACTIVE)
    .map(Order::getTotalAmount)
    .ifPresentOrElse(
        amount -> processPayment(amount),
        () -> log.warn("Order not found or inactive")
    );

// ❌ 引数・フィールド・コレクションでの使用禁止
// public void processOrder(Optional<String> customerId) { }
// private Optional<String> name;
// Optional<List<User>> users;
```

### Stream API使用規則
```java
// ✅ 簡潔で意図が明確な場合のみ
Map<Department, Double> avgSalaries = employees.stream()
    .collect(groupingBy(
        Employee::getDepartment,
        averagingDouble(Employee::getSalary)
    ));

// ✅ メソッド参照優先
users.stream()
    .filter(User::isActive)
    .map(User::getName)
    .forEach(System.out::println);

// ❌ 複雑なラムダ式は禁止
// users.stream().filter(u -> {
//     // 複数行の複雑なロジック
// });

// ✅ 複雑な処理は別メソッドに抽出
users.stream()
    .filter(this::isEligibleForPromotion)
    .forEach(this::sendPromotionNotification);

private boolean isEligibleForPromotion(User user) {
    // 複雑なロジック
}
```

### モダンJava機能活用
```java
// ✅ Record（不変DTO・値オブジェクト）
public record CustomerSummary(
    String name,
    String email,
    BigDecimal totalPurchases,
    LocalDateTime lastLoginAt
) {
    public CustomerSummary {
        Objects.requireNonNull(name, "Name is required");
        Objects.requireNonNull(email, "Email is required");
    }
}

// ✅ Switch式
public String getOrderStatusMessage(OrderStatus status) {
    return switch (status) {
        case PENDING -> "Your order is being processed";
        case SHIPPED -> "Your order has been shipped";
        case DELIVERED -> "Your order has been delivered";
        case CANCELLED -> "Your order has been cancelled";
    };
}

// ✅ Pattern Matching (instanceof)
public double calculateArea(Shape shape) {
    return switch (shape) {
        case Circle c -> Math.PI * c.radius() * c.radius();
        case Rectangle r -> r.width() * r.height();
        case Square s -> s.side() * s.side();
    };
}

// ✅ Text Blocks（必要時）
private static final String SQL_QUERY = """
    SELECT c.id, c.name, c.email, 
           COUNT(o.id) as order_count
    FROM customers c
    LEFT JOIN orders o ON c.id = o.customer_id
    WHERE c.active = true
    GROUP BY c.id, c.name, c.email
    ORDER BY order_count DESC
    """;
```

---

## 🚫 厳格な禁止事項

### 絶対禁止
```java
// ❌ null返却禁止
// public User findUser(String id) { return null; }

// ❌ 例外握り潰し禁止
// try { riskyOperation(); } catch (Exception e) { /* 空 */ }

// ❌ マジックナンバー・文字列禁止
// if (user.getAge() > 65) { /* ... */ }

// ❌ フィールドインジェクション禁止
// @Autowired private UserService userService;

// ❌ 汎用例外禁止
// throw new Exception("Something went wrong");
// catch (Exception e) { /* ... */ }

// ❌ Optional.get()直接呼び出し禁止
// String name = findUser(id).get().getName();

// ❌ System.out/System.err禁止（ログAPI使用）
// System.out.println("Processing user: " + userId);
```

### 推奨回避
```java
// ⚠️ 長いメソッド（20行超）
// ⚠️ 引数3個以上
// ⚠️ 深いネスト（3層以上）
// ⚠️ 共有可変状態
// ⚠️ コメントアウトコード
```

---

## 📝 コメント・ドキュメンテーション

### Javadoc規則
```java
/**
 * 顧客の年齢に基づいて適用可能な割引率を計算します。
 * 
 * <p>割引率は以下の年齢区分で決定されます：
 * <ul>
 *   <li>65歳以上：シニア割引20%</li>
 *   <li>18歳未満：学生割引10%</li>
 *   <li>その他：割引なし</li>
 * </ul>
 *
 * @param customer 対象顧客（null不可）
 * @param baseAmount 基本金額（正の値必須）
 * @return 割引後金額
 * @throws IllegalArgumentException 引数がnullまたは負の値の場合
 * @since 1.0.0
 */
public BigDecimal calculateDiscountedAmount(Customer customer, BigDecimal baseAmount) {
    // 実装...
}
```

### コメント原則
```java
// ✅ 「なぜ」を説明（設計判断・ビジネスルール）
// レート制限のため、リトライ間隔を指数的に増加させる
private void retryWithExponentialBackoff(Runnable operation) { /* ... */ }

// ❌ 「何をしているか」の説明は不要
// i を 1 増やす
// i++;
```

---

## 🧪 テスト容易性

### 設計原則
```java
// ✅ 依存を外部から注入
public class OrderProcessor {
    private final Clock clock;
    private final OrderRepository repository;
    
    public OrderProcessor(Clock clock, OrderRepository repository) {
        this.clock = Objects.requireNonNull(clock);
        this.repository = Objects.requireNonNull(repository);
    }
    
    public Order processOrder(OrderRequest request) {
        var now = Instant.now(clock);  // テスト可能
        // 処理...
    }
}

// ✅ インターフェース指向
public interface OrderRepository {
    Optional<Order> findById(String orderId);
    void save(Order order);
}
```

### テスト作成指針
```java
// ✅ 命名規則: should_期待結果_when_前提条件
@Test
void should_returnDiscountedPrice_when_customerIsSenior() {
    // Given
    var senior = new Customer("John", 70, "john@example.com");
    var basePrice = new BigDecimal("1000");
    
    // When
    var result = discountCalculator.calculateDiscountedAmount(senior, basePrice);
    
    // Then
    assertThat(result).isEqualByComparingTo(new BigDecimal("800"));
}

@Test
void should_throwException_when_customerIsNull() {
    // When & Then
    assertThatThrownBy(() -> 
        discountCalculator.calculateDiscountedAmount(null, BigDecimal.TEN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Customer cannot be null");
}
```

---

## 📊 ロギング・モニタリング

### ログAPI使用
```java
// ✅ 標準 System.Logger（JDK9+）
public class UserService {
    private static final System.Logger log = 
        System.getLogger(UserService.class.getName());
    
    public User createUser(CreateUserRequest request) {
        log.log(System.Logger.Level.INFO, "Creating user: {0}", request.name());
        
        try {
            var user = userFactory.create(request);
            repository.save(user);
            log.log(System.Logger.Level.INFO, "User created: {0}", user.getId());
            return user;
        } catch (Exception e) {
            log.log(System.Logger.Level.ERROR, 
                "Failed to create user: " + request.name(), e);
            throw new UserCreationException("User creation failed", e);
        }
    }
}

// 🔧 SLF4J指示時
// private static final Logger log = LoggerFactory.getLogger(UserService.class);
// log.info("Creating user: {}", request.name());
```

### セキュリティ考慮
```java
// ✅ 機密情報をログから除外
@Override
public String toString() {
    return "User{id=" + id + ", name=" + name + ", email=" + email + "}";
    // passwordフィールドは意図的に除外
}

// ✅ 構造化ログ
log.info("User operation completed: userId={}, operation={}, duration={}ms", 
         userId, operation, duration);
```

---

## ⚡ パフォーマンス・並行性

### 適切なコレクション選択
```java
// 用途別最適選択
List<String> orderedData = new ArrayList<>();     // インデックスアクセス多
List<String> queueData = new LinkedList<>();      // 先頭・末尾操作多
Set<String> uniqueIds = new HashSet<>();          // 高速重複チェック
Map<String, User> userCache = new HashMap<>();    // 高速検索
Map<String, User> sortedUsers = new TreeMap<>();  // ソート維持
```

### 並行性
```java
// ✅ ExecutorService活用
public class AsyncOrderProcessor {
    private final ExecutorService executor = 
        Executors.newFixedThreadPool(10);
    
    public CompletableFuture<OrderResult> processOrderAsync(OrderRequest request) {
        return CompletableFuture
            .supplyAsync(() -> validateOrder(request), executor)
            .thenApply(this::calculatePricing)
            .thenApply(this::saveOrder);
    }
}

// ✅ 並行コレクション
private final Map<String, User> userCache = new ConcurrentHashMap<>();
```

---

## 📦 パッケージ構成・モジュール設計

### 推奨構造
```
com.company.project/
├── domain/              # ドメインモデル・ビジネスルール
│   ├── model/
│   └── repository/      # インターフェースのみ
├── application/         # ユースケース・アプリケーションサービス
├── infrastructure/      # 外部I/O・リポジトリ実装
└── interfaces/          # API・UI・プレゼンテーション層
    ├── rest/
    └── dto/
```

### モジュール境界（Java 9+）
```java
// module-info.java
module com.company.project {
    requires java.base;
    requires java.logging;
    
    exports com.company.project.api;        // 公開API
    exports com.company.project.domain to   // 限定公開
        com.company.project.application;
    
    // 内部パッケージは非公開
}
```

---

## ✅ 生成コード品質チェックリスト

AIは出力前に以下を必ず確認すること：

### 設計品質
- [ ] 各メソッドは単一責任を持つか
- [ ] booleanフラグ引数がないか
- [ ] 引数は0-2個以内か（3個以上はパラメータオブジェクト）
- [ ] 継承よりコンポジションを選択したか

### エラーハンドリング
- [ ] null返却を避けOptionalを使用したか
- [ ] 具体的例外をcause付きでスローしているか
- [ ] try-with-resourcesでリソース管理しているか

### モダンJava活用
- [ ] recordで不変DTOを表現したか
- [ ] switch式・pattern matchingを適切に使用したか
- [ ] Stream APIは簡潔で意図明確か

### ドキュメンテーション
- [ ] 公開APIにJavadocを付与したか
- [ ] コメントは「なぜ」を説明しているか

### テスト・ログ
- [ ] 依存注入でテスト容易性を確保したか
- [ ] 適切なログAPI（System.Logger/SLF4J）を使用したか
- [ ] 機密情報をログ出力から除外したか

### フォーマット
- [ ] Google Java Styleに従っているか
- [ ] 未使用importがないか
- [ ] `@Override`を適切に付与したか

---

## 📚 まとめ

この規約は以下を実現します：

1. **可読性**: 意図が即座に理解できるコード
2. **保守性**: 安全で容易な変更・拡張
3. **堅牢性**: 例外安全でリソース管理された実装
4. **テスト性**: 依存注入による高いテスタビリティ
5. **現代性**: Java 21の機能を活かしたモダンなコード

これらの原則を厳格に遵守し、妥協のない最高品質のJavaコードを生成してください。