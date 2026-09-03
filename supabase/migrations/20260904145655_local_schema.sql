UPDATE public.bake_ingredients
SET "order" = 1
WHERE "order" IS NULL;

ALTER TABLE public.bake_ingredients
    ALTER COLUMN "order" SET DEFAULT 1;

ALTER TABLE public.bake_ingredients
    ALTER COLUMN "order" SET NOT NULL;



UPDATE public.bake_instructions
SET "order" = 1
WHERE "order" IS NULL;

ALTER TABLE public.bake_instructions
    ALTER COLUMN "order" SET DEFAULT 1;

ALTER TABLE public.bake_instructions
    ALTER COLUMN "order" SET NOT NULL;



UPDATE public.ingredient_delta
SET "order" = 1
WHERE "order" IS NULL;

ALTER TABLE public.ingredient_delta
    ALTER COLUMN "order" SET DEFAULT 1;

ALTER TABLE public.ingredient_delta
    ALTER COLUMN "order" SET NOT NULL;



UPDATE public.instruction_delta
SET "order" = 1
WHERE "order" IS NULL;

ALTER TABLE public.instruction_delta
    ALTER COLUMN "order" SET DEFAULT 1;

ALTER TABLE public.instruction_delta
    ALTER COLUMN "order" SET NOT NULL;