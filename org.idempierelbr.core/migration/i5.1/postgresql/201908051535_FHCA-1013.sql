-- 05/08/2019 15h35min13s BRT
UPDATE C_CITY SET Name = REPLACE(name, 'Moji', 'Mogi') WHERE Name LIKE 'Moji%'
;

SELECT lbr_register_migration_script('201908051535_FHCA-1013.sql') FROM dual;
