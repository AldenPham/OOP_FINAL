#### 1. Liệt kê các loại phòng đang trống:
##### java
```java
  list<room> available = room.stream()
    .filter(r -> r.getRoomStatus() == roomStatus.AVAILABLE)
    .toList();
  available.forEach(room ->
      System.out.println(room)
  );
```
  
#### 2. Các loại phòng được thuê nhiều nhất:
##### java
```java
  Map<roomType, Long> rentalCounts = contracts.stream()
    .filter(c -> c.getStatus() == contractStatus.completed)
    .collect(Collectors.groupingBy(
        c -> c.getRoom().getRoomGroup().getType(),
        Collectors.counting()
    ));

  rentalCounts.forEach((type, count) ->
      System.out.println(type + ": " + count + " lượt thuê")
  );
```
### 3. SỐ lần khách quay trở lại
```java
   Map<customer, Long> list = contracts.stream()
    .filter(c -> c.getStatus() == contractStatus.completed)
    .collect(Collector.groupingBy(
        c -> c.getCustomer(),
        Collectors.counting()
   ));

   list.forEach((customer, count) ->
      System.out.println(customer + ": " + count + " lượt thuê")
   );
```
### 4. doanh thu theo phương thức thanh toán

```java
   list<contracts> completedContracts = contracts.stream()
    .filter(c -> c.getStatus() == contractStatus.completed)
    .toList();
  
   Map<paymentType, long> revenue = invoice.stream()
    .filter(i -> completedContracts.stream()
        .anyMatch(c -> c == getContract())
    .collect(Collector.groupingBy(
      i -> i.getPaymentType() ,
      Collector.summingLong(i -> i.getTotal)
    ));
```
### 5. Tra cứu các tiện ích theo hạng phòng 
```java
   Map<roomType, List<utility>> utilitiesByType = roomGroups.stream()
    .collect(Collectors.groupingBy(
      roomGroup::getType,
      Collectors.flatMapping(
        r -> r.getUtilities().stream(),
        Collectors.toList()
    )));
```
### 6. Tìm hạng phòng được đặt ít nhất
```java
   Map<roomType, Long> leastUsed = contracts.stream()
    .filter(c -> c.getStatus == contractStatus.completed)
    .collect(Collectors.groupingBy(
      c -> c.getRoom().getRoomGroup().getType(),
      Collectors.counting();
    ))
  
   long min = leastUsed.value().stream()
    .mapToLong(Long::longValue)
    .min()
    .orElse(0);
```
10. Xem chi tiết số tiền từng hạng phòng mang lại
11. Thống kê doanh thu theo quý 
12. Tính tổng doanh thu khách đã chi cho khách sạn 
13. Tìm khách hàng quay trở lại nhiều nhất

