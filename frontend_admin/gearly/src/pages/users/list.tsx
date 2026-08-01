import React, { PropsWithChildren } from "react";
import {
  useTranslate,
  HttpError,
  getDefaultFilter,
  useExport,
  useGo,
  useNavigation,
} from "@refinedev/core";
import {
  List,
  useTable,
  DateField,
  FilterDropdown,
  getDefaultSortOrder,
  ExportButton,
} from "@refinedev/antd";
import {
  Table,
  Typography,
  theme,
  InputNumber,
  Input,
  Select,
  Button,
  Tag,
} from "antd";
import { EyeOutlined, SearchOutlined } from "@ant-design/icons";
import { PaginationTotal } from "../../components";
import type { IUser } from "../../interfaces";
import { useLocation } from "react-router";

export const AdminUserList: React.FC = ({ children }: PropsWithChildren) => {
  const t = useTranslate();
  const go = useGo();
  const { showUrl } = useNavigation();
  const { pathname } = useLocation();
  const { token } = theme.useToken();

  const { tableProps, filters, sorters } = useTable<IUser, HttpError>({
    resource: "users",
    syncWithLocation: true,
    filters: {
      initial: [{ field: "fullName", operator: "contains", value: "" }],
    },
    sorters: {
      initial: [{ field: "id", order: "desc" }],
    },
  });

  const { isLoading, triggerExport } = useExport<IUser>({
    resource: "users",
    sorters,
    filters,
    pageSize: 50,
    maxItemCount: 1000,
    mapData: (item) => ({
      id: item.id,
      avatar: item.profileAvatar,
      firstName: item.firstName,
      lastName: item.lastName,
      fullName: item.fullName,
      email: item.email,
      phone: item.phone,
      role: item.role,
      verified: item.verified,
      createdAt: item.createdAt,
      updatedAt: item.updatedAt,
      status: item.status,
    }),
  });

  return (
    <List
      breadcrumb={false}
      headerProps={{
        extra: <ExportButton onClick={triggerExport} loading={isLoading} />,
      }}
    >
      <Table
        {...tableProps}
        rowKey="id"
        scroll={{ x: true }}
        pagination={{
          ...tableProps.pagination,
          showTotal: (total) => (
            <PaginationTotal total={total} entityName="users" />
          ),
        }}
      >
        {/* ID */}
        <Table.Column
          key="id"
          dataIndex="id"
          title="ID #"
          render={(val: string) => <Typography.Text>#{val}</Typography.Text>}
          filterIcon={(filtered) => (
            <SearchOutlined
              style={{ color: filtered ? token.colorPrimary : undefined }}
            />
          )}
          defaultFilteredValue={getDefaultFilter("id", filters, "eq")}
          filterDropdown={(props) => (
            <FilterDropdown {...props}>
              <InputNumber
                addonBefore="#"
                style={{ width: "100%" }}
                placeholder={t("users.filter.id.placeholder", "Filter by ID")}
              />
            </FilterDropdown>
          )}
          sorter
          defaultSortOrder={getDefaultSortOrder("id", sorters)}
        />

        {/* Full Name */}
        <Table.Column
          key="fullName"
          dataIndex="fullName"
          title={t("users.fields.fullName", "Full Name")}
          filterIcon={(filtered) => (
            <SearchOutlined
              style={{ color: filtered ? token.colorPrimary : undefined }}
            />
          )}
          defaultFilteredValue={getDefaultFilter(
            "fullName",
            filters,
            "contains"
          )}
          filterDropdown={(props) => (
            <FilterDropdown {...props}>
              <Input
                style={{ width: "100%" }}
                placeholder={t(
                  "users.filter.fullName.placeholder",
                  "Search name"
                )}
              />
            </FilterDropdown>
          )}
        />

        {/* Email */}
        <Table.Column
          key="email"
          dataIndex="email"
          title={t("users.fields.email", "Email")}
          filterIcon={(filtered) => (
            <SearchOutlined
              style={{ color: filtered ? token.colorPrimary : undefined }}
            />
          )}
          defaultFilteredValue={getDefaultFilter("email", filters, "contains")}
          filterDropdown={(props) => (
            <FilterDropdown {...props}>
              <Input
                placeholder={t(
                  "users.filter.email.placeholder",
                  "Search email..."
                )}
                style={{ width: "100%" }}
              />
            </FilterDropdown>
          )}
        />

        {/* Phone */}
        <Table.Column
          key="phone"
          dataIndex="phone"
          title={t("users.fields.phone", "Phone")}
        />

        {/* Role */}
        <Table.Column
          key="role"
          dataIndex="role"
          title={t("users.fields.role", "Role")}
          filters={[
            { text: "ADMIN", value: "ADMIN" },
            { text: "CUSTOMER", value: "CUSTOMER" },
          ]}
          filteredValue={getDefaultFilter("role", filters, "eq")}
          onFilter={(value, record) => record.role === value}
        />

        {/* Verified */}
        <Table.Column
          key="verified"
          dataIndex="verified"
          title={t("users.fields.verified", "Verified")}
          render={(v: boolean) =>
            v ? <Tag color="green">Yes</Tag> : <Tag color="red">No</Tag>
          }
          sorter
          defaultSortOrder={getDefaultSortOrder("verified", sorters)}
          defaultFilteredValue={getDefaultFilter("verified", filters, "eq")}
          filterDropdown={(props) => (
            <FilterDropdown {...props}>
              <Select
                style={{ width: "100%" }}
                placeholder={t(
                  "users.filter.verified.placeholder",
                  "Filter by Verified"
                )}
                allowClear
              >
                <Select.Option value={true}>
                  {t("users.fields.verified.true", "Yes")}
                </Select.Option>
                <Select.Option value={false}>
                  {t("users.fields.verified.false", "No")}
                </Select.Option>
              </Select>
            </FilterDropdown>
          )}
        />

        {/* Created At */}
        <Table.Column
          key="createdAt"
          dataIndex="createdAt"
          title={t("users.fields.createdAt", "Created At")}
          render={(value) => <DateField value={value} format="LLL" />}
          sorter
          defaultSortOrder={getDefaultSortOrder("createdAt", sorters)}
        />

        {/* Actions */}
        <Table.Column<IUser>
          fixed="right"
          title={t("table.actions", "Actions")}
          render={(_, record) => (
            <Button
              icon={<EyeOutlined />}
              onClick={() =>
                go({
                  to: `${showUrl("users", record.id)}`,
                  query: { to: pathname },
                  options: { keepQuery: true },
                  type: "replace",
                })
              }
            />
          )}
        />
      </Table>
      {children}
    </List>
  );
};
