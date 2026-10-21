-- 011-users_blank_username_to_null.sql
--
-- Users.username is optional. A missing username is stored as NULL (the UI shows the email instead).
-- Before Oct 2026 the Add User page stored an empty string; this turns empty or blank usernames into NULL
-- so there is only one way to say "no username".
--
-- RE-RUNNABLE: only touches rows that still have a blank username.

UPDATE "prism"."Users"
   SET username = NULL
 WHERE username IS NOT NULL
   AND btrim(username::text) = '';
