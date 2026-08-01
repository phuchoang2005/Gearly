// src/components/review/ReviewActions.tsx
import React from "react";
import {
  useTranslate,
  useApiUrl,
  useInvalidate,
  useNotification,
} from "@refinedev/core";
import { CheckCircleOutlined, CloseCircleOutlined } from "@ant-design/icons";
import { Dropdown, Menu } from "antd";
import { TableActionButton } from "../../tableActionButton";
import { IReview } from "../../../interfaces";
// import { TableActionButton } from "../tableActionButton";
// import type { IReview } from "../../interfaces";

type ReviewActionProps = {
  record: IReview;
};

export const ReviewActions: React.FC<ReviewActionProps> = ({ record }) => {
  const t = useTranslate();
  const apiUrl = useApiUrl(); // e.g. http://localhost:8080/api
  const invalidate = useInvalidate(); // to refresh the list
  const { open } = useNotification(); // for toast messages

  const callEndpoint = async (
    action: "approve" | "reject",
    successKey: string,
    errorKey: string
  ) => {
    try {
      const res = await fetch(`${apiUrl}/reviews/${record.id}/${action}`, {
        method: "POST",
      });
      if (!res.ok) {
        throw new Error(await res.text());
      }
      const ok: boolean = await res.json();
      if (ok) {
        if (open) {
          open({ type: "success", message: t(successKey) });
        }
        await invalidate({ resource: "reviews", invalidates: ["list"] });
      } else {
        if (open) {
          open({ type: "error", message: t(errorKey) });
        }
      }
    } catch (err: any) {
      if (open) {
        open({ type: "error", message: err.message || t(errorKey) });
      }
    }
  };

  const menu = (
    <Menu onClick={(e) => e.domEvent.stopPropagation()} selectable={false}>
      <Menu.Item
        key="approve"
        icon={<CheckCircleOutlined style={{ color: "#52c41a" }} />}
        disabled={record.status !== "PENDING"}
        onClick={() =>
          callEndpoint(
            "approve",
            "Review Approved Success",
            "Review Approved Error"
          )
        }
      >
        {t("buttons.approve", "Approve")}
      </Menu.Item>
      <Menu.Item
        key="reject"
        icon={<CloseCircleOutlined style={{ color: "#EE2A1E" }} />}
        disabled={record.status !== "PENDING"}
        onClick={() =>
          callEndpoint(
            "reject",
            "Review Rejected Success",
            "Review Rejected Error"
          )
        }
      >
        {t("buttons.reject", "Reject")}
      </Menu.Item>
    </Menu>
  );

  return (
    <Dropdown overlay={menu} trigger={["click"]}>
      <TableActionButton />
    </Dropdown>
  );
};
