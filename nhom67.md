# HOTEL-MANAGEMENT-SYSTEM (HMS)
## Thông Tin Nhóm
#### Chủ đề: Hệ thống quản lý khách sạn
#### Phạm Thành Tín - 25133074
#### Đoàn Thành khiết - 25133031


## Trả lời câu hỏi
#### **1.** _Xác định các thực thể  / các tính khóa / mô tả attribute_ 

##### (1). Khách hàng
* Id Khách hàng 
* name
* phone number
* CCCD

##### (2). Nhóm khách hàng 
* Id Nhóm Khách Hàng
* Hạng khách hàng (vip, regular, normal, long-stay)

##### (3). Phòng
* Vị trí phòng
* Trạng thái phòng (available, not_available ,booked, in service)
* Id phòng
* Giá tiền
* Sức chứa

##### (4). Nhóm phòng
* Mã nhóm phòng
* Tên nhóm
* Loại phòng (deluxe, president, standard) 
##### (5). Hợp đồng thuê phòng
* Thời gian thuê //
* Thời gian nhận/ trả. //
* Phí đi kèm //
* Policy nếu overstay //Overstay Price
* Ai chịu trách nhiệm //Room and Customer 
##### (6). Phiếu tính tiền
* Phương thức tính tiền (CASH, CARD)
* Người trả 
* Cọc (tổng giá tiền phòng * 10% = cọc)
* Trả cho hợp đồng nào
##### (7). Tiện ích
* Mã tiện ích
* Phí tiện ích
* Free cho loại khách hàng nào (Đang ở phòng deluxe+ hoặc là khác hàng regular+)



#### **2.** _Xác định mối liên kết giữa các thực thể từ đó đưa ra lược đồ quan hệ thực thể kết hợp_ 

![Lược đồ](/LucoDo.jpg)

##### (1). Khách hàng
* Id Khách hàng __*(PrimaryKey)*__ 
* name
* phone number
* CCCD

##### (2). Nhóm khách hàng 
* Id Nhóm Khách Hàng __*(PrimaryKey)*__ 
* Hạng khách hàng (vip, regular, normal, long-stay)

##### (3). Phòng
* Vị trí phòng
* Trạng thái phòng (available, not_available ,booked, in service)
* Id phòng __*(PrimaryKey)*__ 
* Giá tiền
* Sức chứa

##### (4). Nhóm phòng
* Mã nhóm phòng __*(PrimaryKey)*__ 
* Tên nhóm
* Loại phòng (deluxe, president, standard) 
##### (5). Hợp đồng thuê phòng
* Thời gian thuê
* Thời gian nhận/ trả.
* Phí đi kèm
* Policy nếu overstay
* Ai chịu trách nhiệm __*(PrimaryKey)*__ 
##### (6). Phiếu tính tiền
* Phương thức tính tiền (CASH, CARD)
* Cọc (tổng giá tiền phòng * 10% = cọc)
* Người trả
* Trả cho hợp đồng nào __*(PrimaryKey)*__ 
##### (7). Tiện ích
* Mã tiện ích __*(PrimaryKey)*__ 
* Phí tiện ích
* Free cho loại khách hàng nào (Đang ở phòng deluxe+ hoặc là khác hàng regular+) 



