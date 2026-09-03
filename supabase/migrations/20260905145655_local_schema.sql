TRUNCATE TABLE public.bake_ingredients CASCADE;

ALTER TABLE public.bake_ingredients
    ALTER COLUMN "amount" SET NOT NULL;

ALTER TABLE public.bake_ingredients
    ALTER COLUMN "name" SET NOT NULL;

TRUNCATE TABLE public.bake_instructions CASCADE;

ALTER TABLE public.bake_instructions
    ALTER COLUMN "description" SET NOT NULL;
