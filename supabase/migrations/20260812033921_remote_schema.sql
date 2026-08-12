-- Migration unit 1: schema_changes
-- Transaction mode: transactional
-- Boundary reason: default

ALTER TABLE public.instruction_delta
  ADD COLUMN version smallint NOT NULL;
