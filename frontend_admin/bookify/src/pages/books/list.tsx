import { useGo, useNavigation, useTranslate } from "@refinedev/core";
import { CreateButton, List } from "@refinedev/antd";
import { ProductListCard, ProductListTable } from "../../components";
import { type PropsWithChildren, useState } from "react";
import { AppstoreOutlined, UnorderedListOutlined } from "@ant-design/icons";
import { Segmented } from "antd";
import { useLocation } from "react-router";
import { BookListTable } from "../../components/book";
import {
  ImportBookCsvButton,
  ImageUploadButton,
} from "../../components/importCsv";

type View = "table" | "card";

export const BookList = ({ children }: PropsWithChildren) => {
  const go = useGo();
  const { replace } = useNavigation();
  const { pathname } = useLocation();
  const { createUrl } = useNavigation();

  const [view, setView] = useState<View>(
    (localStorage.getItem("book-view") as View) || "table"
  );

  const handleViewChange = (value: View) => {
    // remove query params (pagination, filters, etc.) when changing view
    replace("");

    setView(value);
    localStorage.setItem("book-view", value);
  };

  const t = useTranslate();

  return (
    <List
      breadcrumb={false}
      // headerProps={{
      //   extra: ,
      // }}
      headerButtons={(props) => [
        <Segmented<View>
          key="view"
          size="large"
          value={view}
          style={{ marginRight: 24 }}
          options={[
            {
              label: "",
              value: "table",
              icon: <UnorderedListOutlined />,
            },
            {
              label: "",
              value: "card",
              icon: <AppstoreOutlined />,
            },
          ]}
          onChange={handleViewChange}
        />,
        <CreateButton
          {...props.createButtonProps}
          key="create"
          size="large"
          onClick={() => {
            return go({
              to: `${createUrl("books")}`,
              query: {
                to: pathname,
              },
              options: {
                keepQuery: true,
              },
              type: "replace",
            });
          }}
        >
          {t("books.actions.add", "Add")}
        </CreateButton>,
        <ImportBookCsvButton />,
        <ImageUploadButton />,
      ]}
    >
      {view === "table" && <BookListTable />}
      {/*{view === "card" && <ProductListCard />}*/}
      {children}
    </List>
  );
};
