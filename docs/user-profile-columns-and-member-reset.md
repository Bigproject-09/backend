# Users 확장 컬럼 + MEMBER 초기화 SQL

## 1) users 컬럼 추가

```sql
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS name VARCHAR(100) NOT NULL DEFAULT '' AFTER password,
    ADD COLUMN IF NOT EXISTS department VARCHAR(100) NOT NULL DEFAULT '' AFTER name,
    ADD COLUMN IF NOT EXISTS position VARCHAR(100) NOT NULL DEFAULT '' AFTER department;

UPDATE users
SET
    name = CASE WHEN name = '' THEN 'admin' ELSE name END,
    department = CASE WHEN department = '' THEN 'admin' ELSE department END,
    position = CASE WHEN position = '' THEN 'admin' ELSE position END
WHERE role = 0;
```

- role ordinal: `ADMIN=0`, `MEMBER=1`

## 2) MEMBER만 초기화 (ADMIN 유지)

```sql
START TRANSACTION;

CREATE TEMPORARY TABLE tmp_member_ids AS
SELECT user_id
FROM users
WHERE role = 1;

CREATE TEMPORARY TABLE tmp_member_proposal_ids AS
SELECT proposal_id
FROM proposals
WHERE user_id IN (SELECT user_id FROM tmp_member_ids);

CREATE TEMPORARY TABLE tmp_member_presentation_ids AS
SELECT presentation_id
FROM presentations
WHERE proposal_id IN (SELECT proposal_id FROM tmp_member_proposal_ids);

CREATE TEMPORARY TABLE tmp_member_script_ids AS
SELECT script_id
FROM scripts
WHERE presentation_id IN (SELECT presentation_id FROM tmp_member_presentation_ids);

DELETE FROM artifacts
WHERE script_id IN (SELECT script_id FROM tmp_member_script_ids)
   OR presentation_id IN (SELECT presentation_id FROM tmp_member_presentation_ids);

DELETE FROM scripts
WHERE script_id IN (SELECT script_id FROM tmp_member_script_ids);

DELETE FROM presentations
WHERE presentation_id IN (SELECT presentation_id FROM tmp_member_presentation_ids);

DELETE FROM project_members
WHERE user_id IN (SELECT user_id FROM tmp_member_ids)
   OR proposal_id IN (SELECT proposal_id FROM tmp_member_proposal_ids);

DELETE FROM notice_attachments
WHERE user_id IN (SELECT user_id FROM tmp_member_ids);

DELETE FROM proposals
WHERE proposal_id IN (SELECT proposal_id FROM tmp_member_proposal_ids);

DELETE FROM audit_logs
WHERE user_id IN (SELECT user_id FROM tmp_member_ids);

DELETE FROM users
WHERE user_id IN (SELECT user_id FROM tmp_member_ids);

DROP TEMPORARY TABLE tmp_member_script_ids;
DROP TEMPORARY TABLE tmp_member_presentation_ids;
DROP TEMPORARY TABLE tmp_member_proposal_ids;
DROP TEMPORARY TABLE tmp_member_ids;

COMMIT;
```
