-- Migration unit 1: schema_changes
-- Transaction mode: transactional
-- Boundary reason: default

ALTER TABLE public.bake_ingredients
  ALTER COLUMN amount SET DEFAULT ''::text;

ALTER TABLE public.bake_ingredients
  ALTER COLUMN name SET DEFAULT ''::text;

ALTER TABLE public.bake_instructions
  ALTER COLUMN description SET DEFAULT ''::text;

ALTER TABLE public.bake_instructions
  ALTER COLUMN notes SET DEFAULT ''::text;
