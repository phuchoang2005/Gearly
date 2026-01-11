// src/components/review/ReviewListTable.tsx
import React from "react";
import {
  HttpError,
  getDefaultFilter,
  useGo,
  useNavigation,
  useTranslate,
} from "@refinedev/core";
import {
  FilterDropdown,
  getDefaultSortOrder,
  useSelect,
  NumberField,
  useTable,
} from "@refinedev/antd";
import type { IReview, ReviewStatus } from "../../../interfaces";
import { Button, Input, Select, Table, Typography, theme, Tag } from "antd";
import { EyeOutlined, SearchOutlined } from "@ant-design/icons";
import { useLocation } from "react-router";
import { PaginationTotal } from "../../paginationTotal";
import { ReviewActions } from "../review-actions";

export const ReviewListTable: React.FC = () => {
  const { token } = theme.useToken();
  const t = useTranslate();
  const go = useGo();
  const { showUrl } = useNavigation();
  const { pathname } = useLocation();

  const { tableProps, sorters, filters } = useTable<IReview, HttpError>({
    resource: "reviews",
    filters: {
      initial: [
        { field: "bookTitle", operator: "contains", value: "" },
        { field: "orderId", operator: "contains", value: "" },
        { field: "userName", operator: "contains", value: "" },
        { field: "subject", operator: "contains", value: "" },
        { field: "status", operator: "in", value: [] as ReviewStatus[] },
      ],
    },
  });

  // const { selectProps: statusSelectProps } = useSelect<{
  //   text: ReviewStatus;
  // }>({
  //   resource: "reviewStatuses", // register this in your <Refine resources=[…] />
  //   optionLabel: "text",
  //   optionValue: "text",
  //   defaultValue: getDefaultFilter("status", filters, "in"),
  // });

  return (
    <Table
      {...tableProps}
      rowKey="id"
      scroll={{ x: true }}
      pagination={{
        ...tableProps.pagination,
        showTotal: (total) => (
          <PaginationTotal total={total} entityName="reviews" />
        ),
      }}
    >
      {/* ID */}
      {/*<Table.Column*/}
      {/*  title={t("reviews.fields.id", "ID")}*/}
      {/*  dataIndex="id"*/}
      {/*  key="id"*/}
      {/*  width={80}*/}
      {/*  render={(id) => <Typography.Text>#{id}</Typography.Text>}*/}
      {/*/>*/}

      {/* Book ID */}
      <Table.Column
        title={t("reviews.fields.bookTitle", "Book ID")}
        dataIndex="bookTitle"
        key="bookTitle"
        filterIcon={(filtered) => (
          <SearchOutlined
            style={{ color: filtered ? token.colorPrimary : undefined }}
          />
        )}
        defaultFilteredValue={getDefaultFilter(
          "bookTitle",
          filters,
          "contains"
        )}
        filterDropdown={(props) => (
          <FilterDropdown {...props}>
            <Input
              placeholder={t("reviews.filter.bookTitle", "Filter by book")}
            />
          </FilterDropdown>
        )}
      />

      {/* Order ID */}
      {/*<Table.Column*/}
      {/*  title={t("reviews.fields.orderId", "Order ID")}*/}
      {/*  dataIndex="orderId"*/}
      {/*  key="orderId"*/}
      {/*  filterIcon={(filtered) => (*/}
      {/*    <SearchOutlined*/}
      {/*      style={{ color: filtered ? token.colorPrimary : undefined }}*/}
      {/*    />*/}
      {/*  )}*/}
      {/*  defaultFilteredValue={getDefaultFilter("orderId", filters, "contains")}*/}
      {/*  filterDropdown={(props) => (*/}
      {/*    <FilterDropdown {...props}>*/}
      {/*      <Input*/}
      {/*        placeholder={t("reviews.filter.orderId", "Filter by order")}*/}
      {/*      />*/}
      {/*    </FilterDropdown>*/}
      {/*  )}*/}
      {/*/>*/}

      {/* User ID */}
      <Table.Column
        title={t("reviews.fields.userName", "User ID")}
        dataIndex="userName"
        key="userName"
        filterIcon={(filtered) => (
          <SearchOutlined
            style={{ color: filtered ? token.colorPrimary : undefined }}
          />
        )}
        defaultFilteredValue={getDefaultFilter("userName", filters, "contains")}
        filterDropdown={(props) => (
          <FilterDropdown {...props}>
            <Input
              placeholder={t("reviews.filter.userName", "Filter by user")}
            />
          </FilterDropdown>
        )}
      />

      {/* Rating */}
      <Table.Column
        title={t("reviews.fields.rating", "Rating")}
        dataIndex="rating"
        key="rating"
        align="right"
        sorter
        defaultSortOrder={getDefaultSortOrder("rating", sorters)}
        render={(value: number) => <NumberField value={value} />}
      />

      {/* Subject */}
      <Table.Column
        title={t("reviews.fields.subject", "Subject")}
        dataIndex="subject"
        key="subject"
        // filterIcon={(filtered) => (
        //   <SearchOutlined
        //     style={{ color: filtered ? token.colorPrimary : undefined }}
        //   />
        // )}
        // defaultFilteredValue={getDefaultFilter("subject", filters, "contains")}
        // filterDropdown={(props) => (
        //   <FilterDropdown {...props}>
        //     <Input
        //       placeholder={t("reviews.filter.subject", "Filter by subject")}
        //     />
        //   </FilterDropdown>
        // )}
        // render={(text) => (
        //   <Typography.Text ellipsis={{ tooltip: text }}>{text}</Typography.Text>
        // )}
        filterIcon={(filtered) => (
          <SearchOutlined
            style={{ color: filtered ? token.colorPrimary : undefined }}
          />
        )}
        defaultFilteredValue={getDefaultFilter("subject", filters, "contains")}
        filterDropdown={(props) => (
          <FilterDropdown {...props}>
            <Input
              placeholder={t("reviews.filter.subject", "Filter by subject")}
            />
          </FilterDropdown>
        )}
        render={(text) => (
          <Typography.Paragraph
            style={{ margin: 0, whiteSpace: "normal", wordBreak: "break-word" }}
            ellipsis={false}
          >
            {text}
          </Typography.Paragraph>
        )}
      />

      {/* Comment */}
      <Table.Column
        title={t("reviews.fields.comment", "Comment")}
        dataIndex="comment"
        key="comment"
        // render={(text) => (
        //   <Typography.Text ellipsis={{ tooltip: text }}>{text}</Typography.Text>
        // )}
        render={(text) => (
          <Typography.Paragraph
            style={{ margin: 0, whiteSpace: "normal", wordBreak: "break-word" }}
            ellipsis={false}
          >
            {text}
          </Typography.Paragraph>
        )}
      />

      {/* Status */}
      <Table.Column<IReview>
        title={t("reviews.fields.status", "Status")}
        dataIndex="status"
        key="status"
        defaultFilteredValue={getDefaultFilter("status", filters, "in")}
        sorter
        defaultSortOrder={getDefaultSortOrder("status", sorters)}
        filterDropdown={(props) => (
          <FilterDropdown {...props}>
            <Select
              style={{ width: 200 }}
              mode="multiple"
              allowClear
              placeholder={t("reviews.filter.status", "Filter status")}
            />
          </FilterDropdown>
        )}
        render={(status: ReviewStatus) => {
          let color: string;
          switch (status) {
            case "PENDING":
              color = "orange";
              break;
            case "APPROVED":
              color = "green";
              break;
            case "REJECTED":
              color = "red";
              break;
            default:
              color = "default";
          }
          return (
            <Tag color={color}>
              {t(`enum.reviewStatuses.${status}`, status)}
            </Tag>
          );
        }}
      />

      {/* Created At */}
      {/*<Table.Column*/}
      {/*  title={t("reviews.fields.addedAt", "Created At")}*/}
      {/*  dataIndex="addedAt"*/}
      {/*  key="addedAt"*/}
      {/*  render={(value) => (*/}
      {/*    <Typography.Text>{new Date(value).toLocaleString()}</Typography.Text>*/}
      {/*  )}*/}
      {/*  sorter*/}
      {/*/>*/}

      {/* Actions */}
      {/*<Table.Column*/}
      {/*  title={t("table.actions", "Actions")}*/}
      {/*  key="actions"*/}
      {/*  fixed="right"*/}
      {/*  align="center"*/}
      {/*  render={(_, record: IReview) => (*/}
      {/*    <Button*/}
      {/*      icon={<EyeOutlined />}*/}
      {/*      onClick={() =>*/}
      {/*        go({*/}
      {/*          to: `${showUrl("reviews", record.id)}`,*/}
      {/*          query: { to: pathname },*/}
      {/*          options: { keepQuery: true },*/}
      {/*          type: "replace",*/}
      {/*        })*/}
      {/*      }*/}
      {/*    />*/}
      {/*  )}*/}
      {/*/>*/}
      <Table.Column<IReview>
        fixed="right"
        title={t("table.actions", "Actions")}
        key="actions"
        render={(_, record) => <ReviewActions record={record} />}
      />
    </Table>
  );
};
