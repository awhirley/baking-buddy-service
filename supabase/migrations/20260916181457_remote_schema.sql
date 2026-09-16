-- Migration unit 1: schema_changes
-- Transaction mode: transactional
-- Boundary reason: default

ALTER TABLE public.recipes
  ADD COLUMN display_image uuid;

ALTER TABLE public.recipes
  ADD CONSTRAINT recipes_display_image_fkey FOREIGN KEY (display_image) REFERENCES public.bake_images(id) ON UPDATE CASCADE ON DELETE SET NULL;
