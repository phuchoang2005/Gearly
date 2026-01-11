import React from "react";
import { Authenticated, Refine } from "@refinedev/core";
import { RefineKbarProvider, RefineKbar } from "@refinedev/kbar";
import {
  useNotificationProvider,
  ThemedLayoutV2,
  ErrorComponent,
} from "@refinedev/antd";
import routerProvider, {
  CatchAllNavigate,
  NavigateToResource,
  UnsavedChangesNotifier,
  DocumentTitleHandler,
} from "@refinedev/react-router";
import { BrowserRouter, Routes, Route, Outlet } from "react-router";
import {
  ShoppingOutlined,
  ShopOutlined,
  DashboardOutlined,
  UserOutlined,
  UnorderedListOutlined,
  TagsOutlined,
  FormOutlined,
} from "@ant-design/icons";
import jsonServerDataProvider from "@refinedev/simple-rest";
import { authProvider } from "./authProvider";

import "dayjs/locale/de";

import { DashboardPage } from "./pages/dashboard";
import { OrderCreate, OrderList, OrderShow } from "./pages/orders";
import { AuthPage } from "./pages/auth";
import { CustomerShow, CustomerList } from "./pages/customers";
import {
  ProductList,
  ProductCreate,
  ProductEdit,
  ProductShow,
} from "./pages/products";
import { StoreCreate, StoreEdit, StoreList } from "./pages/stores";
import { CategoryList } from "./pages/categories";
import { useTranslation } from "react-i18next";
import { Header, Title } from "./components";
// import { BikeWhiteIcon } from "./components/icons";
import { ConfigProvider } from "./context";
import { useAutoLoginForDemo } from "./hooks";

import "@refinedev/antd/dist/reset.css";
import { BookCreate, BookEdit, BookList, BookShow } from "./pages/books";
import { AdminUserList, UserShow } from "./pages/users";
import { ReviewList } from "./pages/reviews";

const App: React.FC = () => {
  // This hook is used to automatically login the user.
  // We use this hook to skip the login page and demonstrate the application more quickly.
  const { loading } = useAutoLoginForDemo();

  // const API_URL = "https://api.finefoods.refine.dev";
  const API_URL = "http://localhost:8080/api/admin";
  const dataProvider = jsonServerDataProvider(API_URL);

  const { t, i18n } = useTranslation();

  const i18nProvider = {
    translate: (key: string, params: object) => t(key, params),
    changeLocale: (lang: string) => i18n.changeLanguage(lang),
    getLocale: () => i18n.language,
  };

  if (loading) {
    return null;
  }

  return (
    <BrowserRouter>
      <ConfigProvider>
        <RefineKbarProvider>
          <Refine
            routerProvider={routerProvider}
            dataProvider={dataProvider}
            authProvider={authProvider}
            i18nProvider={i18nProvider}
            options={{
              syncWithLocation: true,
              warnWhenUnsavedChanges: true,
            }}
            notificationProvider={useNotificationProvider}
            resources={[
              {
                name: "dashboard",
                list: "/",
                meta: {
                  label: "Dashboard",
                  icon: <DashboardOutlined />,
                },
              },
              {
                name: "orders",
                create: "/orders/new",
                list: "/orders",
                show: "/orders/:id",
                meta: {
                  icon: <ShoppingOutlined />,
                },
              },
              // {
              //   name: "users",
              //   list: "/customers",
              //   show: "/customers/:id",
              //   meta: {
              //     icon: <UserOutlined />,
              //   },
              // },
              {
                name: "users",
                list: "/users",
                show: "/users/:id",
                meta: {
                  icon: <UserOutlined />,
                },
              },
              {
                name: "reviews",
                list: "/reviews",
                meta: {
                  icon: <FormOutlined />,
                },
              },
              {
                name: "books",
                list: "/books",
                create: "/books/new",
                edit: "/books/:id/edit",
                show: "/books/:id",
                meta: {
                  icon: <TagsOutlined />,
                },
              },
              // {
              //   name: "categories",
              //   list: "/categories",
              //   meta: {
              //     icon: <TagsOutlined />,
              //   },
              // },
              // {
              //   name: "stores",
              //   list: "/stores",
              //   create: "/stores/new",
              //   edit: "/stores/:id/edit",
              //   meta: {
              //     icon: <ShopOutlined />,
              //   },
              // },
              // {
              //   name: "couriers",
              //   list: "/couriers",
              //   create: "/couriers/new",
              //   edit: "/couriers/:id/edit",
              //   show: "/couriers/:id",
              //   meta: {
              //     icon: <BikeWhiteIcon />,
              //   },
              // },
            ]}
          >
            <Routes>
              <Route
                element={
                  <Authenticated
                    key="authenticated-routes"
                    fallback={<CatchAllNavigate to="/login" />}
                  >
                    <ThemedLayoutV2 Header={Header} Title={Title}>
                      <div
                        style={{
                          maxWidth: "1200px",
                          marginLeft: "auto",
                          marginRight: "auto",
                        }}
                      >
                        <Outlet />
                      </div>
                    </ThemedLayoutV2>
                  </Authenticated>
                }
              >
                {/* Dashboard */}
                <Route index element={<DashboardPage />} />
                {/* End Dashboard*/}
                {/*<Route path="/orders">*/}
                {/*    <Route index element={<OrderList/>}/>*/}
                {/*    <Route path=":id" element={<OrderShow/>}/>*/}
                {/*</Route>*/}

                <Route path="/orders">
                  <Route index element={<OrderList />} />
                  <Route path=":id" element={<OrderShow />} />
                  <Route path="new" element={<OrderCreate />} />
                </Route>

                <Route
                  path="/customers"
                  element={
                    <CustomerList>
                      <Outlet />
                    </CustomerList>
                  }
                >
                  <Route path=":id" element={<CustomerShow />} />
                </Route>

                <Route
                  path="/users"
                  element={
                    <AdminUserList>
                      <Outlet />
                    </AdminUserList>
                  }
                >
                  <Route path=":id" element={<UserShow />} />
                </Route>

                <Route
                  path="/products"
                  element={
                    <ProductList>
                      <Outlet />
                    </ProductList>
                  }
                >
                  <Route path="new" element={<ProductCreate />} />
                  <Route path=":id" element={<ProductShow />} />
                  <Route path=":id/edit" element={<ProductEdit />} />
                </Route>

                <Route
                  path="/books"
                  element={
                    <BookList>
                      <Outlet />
                    </BookList>
                  }
                >
                  <Route path="new" element={<BookCreate />} />
                  <Route path=":id" element={<BookShow />} />
                  <Route path=":id/edit" element={<BookEdit />} />
                </Route>

                <Route
                  path="/reviews"
                  element={
                    <ReviewList>
                      <Outlet />
                    </ReviewList>
                  }
                >
                  <Route path="new" element={<BookCreate />} />
                  <Route path=":id" element={<BookShow />} />
                  <Route path=":id/edit" element={<BookEdit />} />
                </Route>

                {/*<Route path="/stores">*/}
                {/*  <Route index element={<StoreList />} />*/}
                {/*  <Route path="new" element={<StoreCreate />} />*/}
                {/*  <Route path=":id/edit" element={<StoreEdit />} />*/}
                {/*</Route>*/}

                <Route path="/categories" element={<CategoryList />} />

                {/*<Route path="/couriers">*/}
                {/*  <Route*/}
                {/*    path=""*/}
                {/*    element={*/}
                {/*      <CourierList>*/}
                {/*        <Outlet />*/}
                {/*      </CourierList>*/}
                {/*    }*/}
                {/*  >*/}
                {/*    <Route path="new" element={<CourierCreate />} />*/}
                {/*  </Route>*/}

                {/*  <Route path=":id/edit" element={<CourierEdit />} />*/}
                {/*</Route>*/}
              </Route>

              <Route
                element={
                  <Authenticated key="auth-pages" fallback={<Outlet />}>
                    <NavigateToResource resource="dashboard" />
                  </Authenticated>
                }
              >
                <Route
                  path="/login"
                  element={
                    <AuthPage
                      type="login"
                      formProps={{
                        initialValues: {
                          email: "demo@refine.dev",
                          password: "demodemo",
                        },
                      }}
                    />
                  }
                />
                <Route
                  path="/register"
                  element={
                    <AuthPage
                      type="register"
                      formProps={{
                        initialValues: {
                          email: "demo@refine.dev",
                          password: "demodemo",
                        },
                      }}
                    />
                  }
                />
                <Route
                  path="/forgot-password"
                  element={<AuthPage type="forgotPassword" />}
                />
                <Route
                  path="/update-password"
                  element={<AuthPage type="updatePassword" />}
                />
              </Route>

              <Route
                element={
                  <Authenticated key="catch-all">
                    <ThemedLayoutV2 Header={Header} Title={Title}>
                      <Outlet />
                    </ThemedLayoutV2>
                  </Authenticated>
                }
              >
                <Route path="*" element={<ErrorComponent />} />
              </Route>
            </Routes>
            <UnsavedChangesNotifier />
            <RefineKbar />
          </Refine>
        </RefineKbarProvider>
      </ConfigProvider>
    </BrowserRouter>
  );
};

export default App;
