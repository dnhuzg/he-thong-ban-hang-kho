# S8-07 â€“ BÃ¡o cÃ¡o tá»“n kho vÃ  vÃ²ng quay hÃ ng

## Pháº¡m vi backend

Backend cung cáº¥p bÃ¡o cÃ¡o cho Quáº£n lÃ½ kho, gá»“m:

- GiÃ¡ trá»‹ tá»“n theo kho vÃ  theo nhÃ³m hÃ ng.
- Sá»‘ ngÃ y tá»“n bÃ¬nh quÃ¢n cá»§a tá»«ng SKU.
- HÃ ng cháº­m luÃ¢n chuyá»ƒn khi sá»‘ ngÃ y tá»“n bÃ¬nh quÃ¢n tá»« 30 ngÃ y trá»Ÿ lÃªn.
- Xuáº¥t bÃ¡o cÃ¡o Excel `.xlsx`.

Pháº§n giao diá»‡n cÃ³ thá»ƒ tÃ­ch há»£p vÃ o hai endpoint Ä‘Ã£ cung cáº¥p.

## Endpoint

### Xem bÃ¡o cÃ¡o

`GET /reports/inventory`

Query tÃ¹y chá»n:

- `warehouseId`
- `categoryId`

Pháº£n há»“i JSON:

- `valueRows`: tá»•ng sá»‘ lÆ°á»£ng vÃ  giÃ¡ trá»‹ tá»“n theo kho + nhÃ³m hÃ ng.
- `skuAgeRows`: sá»‘ lÆ°á»£ng tá»“n, sá»‘ ngÃ y tá»“n bÃ¬nh quÃ¢n vÃ  cá» cháº­m luÃ¢n chuyá»ƒn.
- `slowMovingThresholdDays`: `30`.

### Xuáº¥t Excel

`GET /reports/inventory/export`

Nháº­n cÃ¹ng query tÃ¹y chá»n vá»›i API bÃ¡o cÃ¡o.

Tá»‡p Excel gá»“m:

- `GiÃ¡ trá»‹ tá»“n`
- `NgÃ y tá»“n SKU`
- `Tham sá»‘`

## TÃ­nh sá»‘ ngÃ y tá»“n

Sá»‘ ngÃ y tá»“n bÃ¬nh quÃ¢n cá»§a SKU Ä‘Æ°á»£c tÃ­nh theo trá»ng sá»‘ sá»‘ lÆ°á»£ng cÃ²n láº¡i cá»§a tá»«ng lÃ´:

`SUM(sá»‘ lÆ°á»£ng lÃ´ Ã— sá»‘ ngÃ y Ä‘Ã£ tá»“n) / SUM(sá»‘ lÆ°á»£ng lÃ´)`

LÃ´ cÃ³ ngÃ y nháº­n á»Ÿ tÆ°Æ¡ng lai Ä‘Æ°á»£c tÃ­nh 0 ngÃ y Ä‘á»ƒ trÃ¡nh sá»‘ Ã¢m.

## Quyá»n

Endpoint yÃªu cáº§u quyá»n Ä‘á»c module `REPORT` vÃ  vai trÃ² `WH_MANAGER` hoáº·c `ADMIN`.

## Migration

`database/migrations/021_bao_cao_ton_kho.sql`

Báº£ng `inventory_lots` lÆ°u lÆ°á»£ng cÃ²n láº¡i cá»§a tá»«ng lÃ´ cÃ¹ng giÃ¡ vá»‘n vÃ  thá»i Ä‘iá»ƒm nháº­n. CÃ¡c nghiá»‡p vá»¥ nháº­p/xuáº¥t/chuyá»ƒn kho vá» sau cáº§n cáº­p nháº­t read model nÃ y Ä‘á»ƒ bÃ¡o cÃ¡o pháº£n Ã¡nh tá»“n thá»±c táº¿.

## Kiá»ƒm thá»­

`InventoryReportServiceTest` kiá»ƒm tra:

- nhÃ³m giÃ¡ trá»‹ tá»“n theo kho/nhÃ³m hÃ ng;
- tÃ­nh ngÃ y tá»“n bÃ¬nh quÃ¢n cÃ³ trá»ng sá»‘;
- Ä‘Ã¡nh dáº¥u cháº­m luÃ¢n chuyá»ƒn á»Ÿ ngÆ°á»¡ng 30 ngÃ y;
- xuáº¥t Ä‘Æ°á»£c tá»‡p `.xlsx`.

## LÆ°u Ã½ nghiá»‡p vá»¥

AC hiá»‡n yÃªu cáº§u sá»‘ ngÃ y tá»“n bÃ¬nh quÃ¢n vÃ  Ä‘Ã¡nh dáº¥u hÃ ng cháº­m luÃ¢n chuyá»ƒn, chÆ°a cung cáº¥p cÃ´ng thá»©c vÃ²ng quay dá»±a trÃªn giÃ¡ vá»‘n xuáº¥t bÃ¡n. VÃ¬ váº­y backend hiá»‡n táº­p trung vÃ o chá»‰ sá»‘ ngÃ y tá»“n vÃ  cá» cháº­m luÃ¢n chuyá»ƒn.