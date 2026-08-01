import React from "react";
import {
  CarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  DollarCircleOutlined,
  SmileOutlined,
  SyncOutlined,
} from "@ant-design/icons";
import { useTranslate } from "@refinedev/core";
import { Tag } from "antd";
import { OrderStatusType } from "../../../interfaces";

type OrderStatusProps = {
  status: OrderStatusType;
};

export const OrderStatus: React.FC<OrderStatusProps> = ({ status }) => {
  const t = useTranslate();

  let color: string;
  let icon: React.ReactNode;

  switch (status) {
    case "PENDING":
      color = "orange";
      icon = <ClockCircleOutlined />;
      break;
    case "PROCESSING":
      color = "blue";
      icon = <SyncOutlined spin />;
      break;
    case "SHIPPED":
      color = "cyan";
      icon = <CarOutlined />;
      break;
    case "DELIVERED":
      color = "green";
      icon = <SmileOutlined />;
      break;
    case "COMPLETED":
      color = "geekblue";
      icon = <CheckCircleOutlined />;
      break;
    case "CANCELLED":
      color = "red";
      icon = <CloseCircleOutlined />;
      break;
    case "REFUNDED":
      color = "purple";
      icon = <DollarCircleOutlined />;
      break;
    default:
      color = "default";
      icon = null;
  }

  return (
    <Tag color={color} icon={icon}>
      {t(`enum.orderStatuses.${status}`, status)}
    </Tag>
  );
};
