DROP TABLE IF EXISTS stories;
CREATE TABLE stories (
    id                  VARCHAR(36)   NOT NULL PRIMARY KEY,
    title               TEXT          NOT NULL,
    theme               TEXT          NOT NULL,
    characters          JSON          NOT NULL,
    age_group           VARCHAR(10)   NOT NULL,
    length              VARCHAR(10)   NOT NULL,
    language            VARCHAR(5)    NOT NULL DEFAULT 'uk',
    content             LONGTEXT      NOT NULL,
    illustration_path_light VARCHAR(500) NULL,
    illustration_path_dark  VARCHAR(500) NULL,
    illustration_status VARCHAR(20)   NOT NULL DEFAULT 'pending',
    created_at          DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);
CREATE INDEX idx_stories_created_at ON stories (created_at DESC);
