-- Bake steps: add/remove ingredients and instructions during a bake.
-- Written for PostgreSQL; adjust syntax if you're on another database.

-- 1. A step added during a bake has no originating delta yet.
ALTER TABLE public.bake_ingredients  ALTER COLUMN ingredient_delta_id  DROP NOT NULL;
ALTER TABLE public.bake_instructions ALTER COLUMN instruction_delta_id DROP NOT NULL;

-- 2. A step removed during a bake stays on the bake row, flagged.
ALTER TABLE public.bake_ingredients  ADD COLUMN omitted boolean NOT NULL DEFAULT false;
ALTER TABLE public.bake_instructions ADD COLUMN omitted boolean NOT NULL DEFAULT false;

-- 3. Backfill: open bakes that were seeded from a recipe step that had been omitted
--    (createBake used to copy omitted steps). Completed bakes are left as history.
UPDATE public.bake_ingredients bi
SET omitted = d.omitted
    FROM public.ingredient_delta d
WHERE d.id = bi.ingredient_delta_id
  AND bi.bake_id IN (SELECT id FROM bakes WHERE end_datetime IS NULL);

UPDATE public.bake_instructions bi
SET omitted = d.omitted
    FROM public.instruction_delta d
WHERE d.id = bi.instruction_delta_id
  AND bi.bake_id IN (SELECT id FROM bakes WHERE end_datetime IS NULL);
