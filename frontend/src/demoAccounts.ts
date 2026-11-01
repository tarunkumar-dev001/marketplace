export const demoAccounts = {
  admin: { email: import.meta.env.VITE_DEMO_ADMIN_EMAIL as string | undefined, password: import.meta.env.VITE_DEMO_ADMIN_PASSWORD as string | undefined },
  vendor: { email: import.meta.env.VITE_DEMO_VENDOR_EMAIL as string | undefined, password: import.meta.env.VITE_DEMO_VENDOR_PASSWORD as string | undefined },
  customer: { email: import.meta.env.VITE_DEMO_CUSTOMER_EMAIL as string | undefined, password: import.meta.env.VITE_DEMO_CUSTOMER_PASSWORD as string | undefined },
}
