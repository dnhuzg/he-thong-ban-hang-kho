# Backend integration fixes

The following frontend packs now use the database through `Servlet -> Service -> DAO` instead of browser demo data:

- GHTK-51: warehouse and storage-location management
- GHTK-55: internal warehouse transfers
- GHTK-56: inventory counts and stock adjustments
- GHTK-70: credit alerts from current receivables
- GHTK-71: route collections, payment allocation, receipt upload and confirmation
- GHTK-72: debt reconciliation and customer response
- GHTK-76: sales returns and returned-stock receiving
- GHTK-77: return credit notes and receivable reduction
- GHTK-85: consolidated debt ledger

## Database setup

Apply the migrations in numeric order after the existing migrations:

1. `database/migrations/029_tra_hang_giam_cong_no.sql`
2. `database/migrations/030_kho_va_vi_tri.sql`
3. `database/migrations/031_chuyen_kho.sql`
4. `database/migrations/032_kiem_ke_kho.sql`
5. `database/migrations/033_thu_tien_theo_tuyen.sql`
6. `database/migrations/034_doi_chieu_cong_no.sql`

The same final table definitions are also included in `database/schema.sql` for a new database installation.

## Verification performed

- Focused Java 17 compilation of the changed models, DAOs, services and servlets.
- JavaScript syntax checks for every changed pack.
- Static scan of the changed pages for `localStorage`, `sessionStorage` and demo/mock data.

## Integration note

Exact return-to-original-lot traceability needs an upstream relation between a shipped invoice line and its inventory lot. The current schema does not store that relation, so returns are restored by warehouse and product while preserving the original invoice-line reference and price.
