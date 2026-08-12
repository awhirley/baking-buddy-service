-- Migration unit 1: schema_changes
-- Transaction mode: transactional
-- Boundary reason: default

ALTER TABLE public.recipes
  DROP COLUMN source;

ALTER TABLE public.recipes
  ADD COLUMN recipe_source text;
