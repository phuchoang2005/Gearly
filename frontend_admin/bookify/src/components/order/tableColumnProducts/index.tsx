import React from "react";
import { Flex, Popover, Typography, Badge, Avatar, theme } from "antd";
import { getUniqueListWithCount } from "../../../utils";
import type { IOrder, IOrderItem } from "../../../interfaces";
import { useTranslate } from "@refinedev/core";

const VISIBLE_ITEM_COUNT = 4;

type Props = {
  order: IOrder;
};

export const OrderTableColumnItems: React.FC<Props> = ({ order }) => {
  const t = useTranslate();
  const { token } = theme.useToken();

  // Aggregate duplicates by bookId
  const uniqueItems = getUniqueListWithCount<IOrderItem>({
    list: order.items || [],
    field: "bookId",
  });
  const visibleItems = uniqueItems.slice(0, VISIBLE_ITEM_COUNT);
  const hiddenItems = uniqueItems.slice(VISIBLE_ITEM_COUNT);

  return (
    <Flex gap={12}>
      {visibleItems.map((item) => (
        <Popover
          key={item.bookId}
          content={<Typography.Text>{item.title}</Typography.Text>}
        >
          <Badge
            count={item.count > 1 ? item.count : 0}
            style={{ color: "#fff" }}
          >
            <Avatar
              shape="square"
              style={{ backgroundColor: token.colorPrimaryBg }}
            >
              {item.title}
            </Avatar>
          </Badge>
        </Popover>
      ))}

      {hiddenItems.length > 0 && (
        <Popover
          title={t("orders.fields.items", "Items")}
          content={
            <Flex gap={8}>
              {hiddenItems.map((item) => (
                <Popover
                  key={item.bookId}
                  content={<Typography.Text>{item.title}</Typography.Text>}
                >
                  <Badge
                    count={item.count > 1 ? item.count : 0}
                    style={{ color: "#fff" }}
                  >
                    <Avatar
                      shape="square"
                      style={{ backgroundColor: token.colorPrimaryBg }}
                    >
                      {item.title.charAt(0).toUpperCase()}
                    </Avatar>
                  </Badge>
                </Popover>
              ))}
            </Flex>
          }
        >
          <Avatar
            shape="square"
            style={{ backgroundColor: token.colorPrimaryBg }}
          >
            <Typography.Text style={{ color: token.colorPrimary }}>
              +{hiddenItems.length}
            </Typography.Text>
          </Avatar>
        </Popover>
      )}
    </Flex>
  );
};
