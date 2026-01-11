import { NumberField, useTable } from "@refinedev/antd";
import type { IUser, IOrder, IOrderFilterVariables } from "../../../interfaces";
import { type HttpError, useNavigation, useTranslate } from "@refinedev/core";
import { Table, Typography } from "antd";
import { OrderStatus, OrderTableColumnItems } from "../../order";

type Props = {
  customer?: IUser;
};

export const CustomerOrderHistory = ({ customer }: Props) => {
  const t = useTranslate();
  const { show } = useNavigation();

  const { tableProps } = useTable<IOrder, HttpError, IOrderFilterVariables>({
    resource: "orders",
    initialSorter: [
      {
        field: "createdAt",
        order: "desc",
      },
    ],
    permanentFilter: [
      {
        field: "user.id",
        operator: "eq",
        value: customer?.id,
      },
    ],
    initialPageSize: 4,
    queryOptions: {
      enabled: customer !== undefined,
    },
    syncWithLocation: false,
  });

  return (
    <Table
      {...tableProps}
      rowKey="id"
      onRow={(record) => {
        return {
          onClick: () => {
            show("orders", record.id);
          },
        };
      }}
      pagination={{
        ...tableProps.pagination,
        hideOnSinglePage: true,
      }}
    >
      <Table.Column
        title={`${t("orders.order")} #`}
        dataIndex="id"
        key="id"
        render={(value) => (
          <Typography.Text
            style={{
              whiteSpace: "nowrap",
            }}
          >
            #{value}
          </Typography.Text>
        )}
      />
      <Table.Column
        key="orderStatus"
        dataIndex="orderStatus"
        title={t("orders.fields.status")}
        render={(value, record) => {
            console.log("Order record:", record);
            return <OrderStatus status={value} />;
        }}
      />
      <Table.Column<IOrder>
        key="products"
        dataIndex="products"
        title={t("orders.fields.products")}
        render={(_, record) => {
          return <OrderTableColumnItems order={record} />;
        }}
      />
      <Table.Column<IOrder>
        dataIndex="amount"
        align="end"
        title={t("orders.fields.amount")}
        render={(amount) => {
          return (
            <NumberField
              value={amount}
              style={{
                whiteSpace: "nowrap",
              }}
              options={{
                style: "currency",
                currency: "USD",
              }}
            />
          );
        }}
      />
      <Table.Column
        key="store.title"
        dataIndex={["store", "title"]}
        title={t("orders.fields.store")}
      />
    </Table>
  );
};
