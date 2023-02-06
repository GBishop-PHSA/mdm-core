UPDATE core.flyway_schema_history
SET checksum = 490568862
WHERE version = '1.7.0' AND
      checksum = -1413608684;

DELETE
FROM core.flyway_schema_history
WHERE version = '2.10.0' AND
      description = 'update database metadata values';

DELETE
FROM core.flyway_schema_history
WHERE version = '5.1.13' AND
      description = 'delete invalid breadcrumb trees';