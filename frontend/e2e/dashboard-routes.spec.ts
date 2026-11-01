import { expect, test } from '@playwright/test';

test('login route renders the marketplace authentication form', async ({ page }) => {
  await page.goto('http://127.0.0.1:5173/login');
  await expect(page.getByRole('heading', { name: /welcome back/i })).toBeVisible();
  await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible();
});

test('protected admin route redirects unauthenticated access', async ({ page }) => {
  await page.goto('http://127.0.0.1:5173/admin/dashboard');
  await expect(page).toHaveURL(/login|admin\/dashboard/);
});
