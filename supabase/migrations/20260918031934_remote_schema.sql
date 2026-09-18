-- Migration unit 1: schema_changes
-- Transaction mode: transactional
-- Boundary reason: default

ALTER TABLE public.ingredient_delta
  ADD COLUMN omitted boolean DEFAULT false NOT NULL;

ALTER TABLE public.instruction_delta
  ADD COLUMN omitted boolean DEFAULT false NOT NULL;
