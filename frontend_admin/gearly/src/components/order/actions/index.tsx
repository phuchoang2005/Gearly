import React from "react";
import {
  useTranslate,
  useApiUrl,
  useInvalidate,
  useNotification,
} from "@refinedev/core";
import {
  SyncOutlined,
  CarOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DollarCircleOutlined,
} from "@ant-design/icons";
import { Dropdown, Menu } from "antd";
import { TableActionButton } from "../../tableActionButton";
import type { IOrder } from "../../../interfaces";

type OrderActionProps = {
  record: IOrder;
};

export const OrderActions: React.FC<OrderActionProps> = ({ record }) => {
  const t = useTranslate();
  const apiUrl = useApiUrl();
  const invalidate = useInvalidate();
  const { open } = useNotification();

  const call = async (action: string, successKey: string, errorKey: string) => {
    try {
      const res = await fetch(`${apiUrl}/orders/${record.id}/${action}`, {
        method: "POST",
      });
      if (!res.ok) throw new Error(await res.text());
      const ok: boolean = await res.json();
      if (ok) {
        if (open) {
          open({ type: "success", message: t(successKey) });
        }
        await invalidate({ resource: "orders", invalidates: ["list"] });
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

  const { orderStatus } = record;

  // Build only the relevant menu items
  const items: React.ReactNode[] = [];

  if (orderStatus === "PENDING") {
    items.push(
      <Menu.Item
        key="process"
        icon={<SyncOutlined spin style={{ color: "#1890ff" }} />}
        onClick={() =>
          call(
            "set-process",
            "orders.notifications.processed",
            "orders.notifications.processError"
          )
        }
      >
        {t("buttons.process", "Process")}
      </Menu.Item>
    );
  }

  if (orderStatus === "PROCESSING") {
    items.push(
      <Menu.Item
        key="ship"
        icon={<CarOutlined style={{ color: "#fa8c16" }} />}
        onClick={() =>
          call(
            "set-ship",
            "orders.notifications.shipped",
            "orders.notifications.shippedError"
          )
        }
      >
        {t("buttons.setShipped", "Shipped")}
      </Menu.Item>
    );
  }

  if (orderStatus === "SHIPPED") {
    items.push(
      <Menu.Item
        key="deliver"
        icon={<CheckCircleOutlined style={{ color: "#52c41a" }} />}
        onClick={() =>
          call(
            "set-delivered",
            "orders.notifications.delivered",
            "orders.notifications.deliveredError"
          )
        }
      >
        {t("buttons.delivered", "Delivered")}
      </Menu.Item>
    );
  }

  if (orderStatus === "DELIVERED") {
    items.push(
      <Menu.Item
        key="pendingRefund"
        icon={<DollarCircleOutlined style={{ color: "#fa8c16" }} />}
        onClick={() =>
          call(
            "set-pending-refund",
            "orders.notifications.pendingRefundSuccess",
            "orders.notifications.pendingRefundError"
          )
        }
      >
        {t("buttons.setPendingRefund", "Pending Refund")}
      </Menu.Item>
    );
  }

  if (orderStatus === "DELIVERED") {
    items.push(
      <Menu.Item
        key="complete"
        icon={<CheckCircleOutlined style={{ color: "#52c41a" }} />}
        onClick={() =>
          call(
            "set-complete",
            "orders.notifications.completeSuccess",
            "orders.notifications.completeError"
          )
        }
      >
        {t("buttons.complete", "Complete")}
      </Menu.Item>
    );
  }

  if (orderStatus === "PENDING_REFUND") {
    items.push(
      <Menu.Item
        key="refund"
        icon={<DollarCircleOutlined style={{ color: "#fa8c16" }} />}
        onClick={() =>
          call(
            "set-refund",
            "orders.notifications.refundSuccess",
            "orders.notifications.refundError"
          )
        }
      >
        {t("buttons.refund", "Refund")}
      </Menu.Item>
    );
  }

  // Always allow cancel unless already CANCELLED
  if (orderStatus === "PENDING" || orderStatus === "PROCESSING") {
    items.push(
      <Menu.Item
        key="cancel"
        icon={<CloseCircleOutlined style={{ color: "#EE2A1E" }} />}
        onClick={() =>
          call(
            "set-cancel",
            "orders.notifications.cancelSuccess",
            "orders.notifications.cancelError"
          )
        }
      >
        {t("buttons.cancel", "Cancel")}
      </Menu.Item>
    );
  }

  return (
    <Dropdown
      overlay={
        <Menu onClick={(e) => e.domEvent.stopPropagation()}>{items}</Menu>
      }
      trigger={["click"]}
    >
      <TableActionButton />
    </Dropdown>
  );
};
