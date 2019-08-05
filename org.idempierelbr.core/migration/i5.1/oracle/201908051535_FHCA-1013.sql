SET SQLBLANKLINES ON
SET DEFINE OFF

-- 01/08/2018 13h57min13s BRT
UPDATE C_CITY SET Name = REPLACE(name, 'Moji', 'Mogi') WHERE Name LIKE 'Moji%'
;

SELECT lbr_register_migration_script('201908051535_FHCA-1013.sql') FROM dual;
