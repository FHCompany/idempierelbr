-- ALTER TABLE LBR_FiscalGroup_BPartner DROP CONSTRAINT LBR_FiscalGroup_BPartner_UU_uu_idx
-- ;

ALTER TABLE LBR_FiscalGroup_BPartner ADD CONSTRAINT LBR_FiscalGroup_BPartne_uu_idx UNIQUE (LBR_FiscalGroup_BPartner_UU)
;

SELECT register_migration_script('201811141038_FixLongIdentifierOracle.sql') FROM dual
;

