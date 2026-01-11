// src/pages/reviews/list.tsx

import React, { useState, PropsWithChildren } from "react";
import { useGo, useNavigation, useTranslate } from "@refinedev/core";
import { List, CreateButton } from "@refinedev/antd";
import { Segmented } from "antd";
import { AppstoreOutlined, UnorderedListOutlined } from "@ant-design/icons";
import { useLocation } from "react-router";
import { ReviewListTable } from "../../components/review";

// import { ReviewListTable } from "../../components/review/ReviewListTable";
// import { ReviewListCard } from "../../components/review/ReviewListCard";
// import { ImportReviewCsvButton } from "../../components/importCsv/ImportReviewCsvButton";

type View = "table" | "card";

export const ReviewList: React.FC<PropsWithChildren> = ({ children }) => {
  const go = useGo();
  const { createUrl, replace } = useNavigation();
  const { pathname } = useLocation();
  const t = useTranslate();

  // remember view choice in localStorage
  const [view, setView] = useState<View>(
    (localStorage.getItem("review-view") as View) || "table"
  );

  const handleViewChange = (value: View) => {
    replace(""); // clear pagination/filters
    setView(value);
    localStorage.setItem("review-view", value);
  };

  return (
    <List
      breadcrumb={false}
      headerButtons={(props) => [
        // 1) toggle table/card
        <Segmented<View>
          key="view"
          size="large"
          value={view}
          style={{ marginRight: 24 }}
          options={[
            { label: "", value: "table", icon: <UnorderedListOutlined /> },
            { label: "", value: "card", icon: <AppstoreOutlined /> },
          ]}
          onChange={handleViewChange}
        />,

        // 2) create new review
        // <CreateButton
        //   {...props.createButtonProps}
        //   key="create"
        //   size="large"
        //   onClick={() =>
        //     go({
        //       to: createUrl("reviews"),
        //       query: { to: pathname },
        //       options: { keepQuery: true },
        //       type: "replace",
        //     })
        //   }
        // >
        //   {t("reviews.actions.add", "Add Review")}
        // </CreateButton>,

        // 3) import bulk via CSV
        // <ImportReviewCsvButton key="import" />,
      ]}
    >
      {view === "table" && <ReviewListTable />}
      {/*{view === "card" && <ReviewListCard />}*/}
      {children}
    </List>
  );
};
