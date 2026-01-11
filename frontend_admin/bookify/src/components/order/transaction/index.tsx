// src/components/transaction/TransactionStatusTag.tsx

import React from "react";
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
} from "@ant-design/icons";
import { Tag } from "antd";
import { useTranslate } from "@refinedev/core";
import { TransactionStatus } from "../../../interfaces";

type Props = {
  status: TransactionStatus;
};

export const TransactionStatusTag: React.FC<Props> = ({ status }) => {
  const t = useTranslate();

  let color: string;
  let icon: React.ReactNode;

  switch (status) {
    case "PENDING":
      color = "orange";
      icon = <ClockCircleOutlined />;
      break;
    case "SUCCESSFUL":
      color = "green";
      icon = <CheckCircleOutlined />;
      break;
    case "FAILED":
      color = "red";
      icon = <CloseCircleOutlined />;
      break;
    default:
      color = "default";
      icon = null;
  }

  return (
    <Tag color={color} icon={icon}>
      {t(`enum.transactionStatuses.${status}`, status)}
    </Tag>
  );
};
