import {
  HttpError,
  getDefaultFilter,
  useGo,
  useNavigation,
  useTranslate,
  useApiUrl,
} from "@refinedev/core";
import {
  FilterDropdown,
  getDefaultSortOrder,
  useSelect,
  NumberField,
  useTable,
} from "@refinedev/antd";
import type { IBook, ICategory } from "../../../interfaces";
import {
  Avatar,
  Button,
  Input,
  InputNumber,
  Select,
  Table,
  Typography,
  theme,
  Tag,
} from "antd";
import { EyeOutlined, SearchOutlined } from "@ant-design/icons";
import { useLocation } from "react-router";
import { PaginationTotal } from "../../paginationTotal";

export const BookListTable: React.FC = () => {
  const { token } = theme.useToken();
  const t = useTranslate();
  const go = useGo();
  const { pathname } = useLocation();
  const { showUrl } = useNavigation();

  const { tableProps, sorters, filters } = useTable<IBook, HttpError>({
    resource: "books",
    filters: {
      initial: [
        { field: "title", operator: "contains", value: "" },
        { field: "authors", operator: "contains", value: "" },
        { field: "categoryIds", operator: "in", value: [] },
      ],
    },
  });

  const { selectProps: categorySelectProps, query: categoryQuery } =
    useSelect<ICategory>({
      resource: "categories",
      optionLabel: "name",
      optionValue: "id",
      defaultValue: getDefaultFilter("categoryIds", filters, "in"),
    });
  const categories = categoryQuery?.data?.data || [];

  const apiUrl = useApiUrl(); // e.g. "http://localhost:8080/api"
  const backendOrigin = apiUrl.replace(/\/api$/, "");

  return (
    <Table
      {...tableProps}
      rowKey="id"
      scroll={{ x: true }}
      pagination={{
        ...tableProps.pagination,
        showTotal: (total) => (
          <PaginationTotal total={total} entityName="books" />
        ),
      }}
    >
      {/* ID */}
      {/*<Table.Column*/}
      {/*    title="ID #"*/}
      {/*    dataIndex="id"*/}
      {/*    key="id"*/}
      {/*    width={80}*/}
      {/*    render={(value) => <Typography.Text>#{value}</Typography.Text>}*/}
      {/*    filterIcon={(filtered) => (*/}
      {/*        <SearchOutlined*/}
      {/*            style={{ color: filtered ? token.colorPrimary : undefined }}*/}
      {/*        />*/}
      {/*    )}*/}
      {/*    defaultFilteredValue={getDefaultFilter("id", filters, "eq")}*/}
      {/*    filterDropdown={(props) => (*/}
      {/*        <FilterDropdown {...props}>*/}
      {/*            <InputNumber*/}
      {/*                addonBefore="#"*/}
      {/*                style={{ width: "100%" }}*/}
      {/*                placeholder="Filter by ID"*/}
      {/*            />*/}
      {/*        </FilterDropdown>*/}
      {/*    )}*/}
      {/*/>*/}

      {/* Cover Image */}
      {/*<Table.Column*/}
      {/*  title="Cover"*/}
      {/*  dataIndex="images"*/}
      {/*  key="images"*/}
      {/*  render={(images: IBook["images"]) => (*/}
      {/*    <Avatar*/}
      {/*      shape="square"*/}
      {/*      // src={images?.[0]?.thumbnailUrl || images?.[0]?.url}*/}
      {/*      src={*/}
      {/*        images?.[0]?.url ? `${backendOrigin}${images[0].url}` : undefined*/}
      {/*      }*/}
      {/*    />*/}
      {/*  )}*/}
      {/*/>*/}

      {/* Title */}
      <Table.Column
        title="Title"
        dataIndex="title"
        key="title"
        filterIcon={(filtered) => (
          <SearchOutlined
            style={{ color: filtered ? token.colorPrimary : undefined }}
          />
        )}
        defaultFilteredValue={getDefaultFilter("title", filters, "contains")}
        filterDropdown={(props) => (
          <FilterDropdown {...props}>
            <Input placeholder="Search title..." />
          </FilterDropdown>
        )}
        render={(text: string) => (
          <Typography.Text ellipsis={{ tooltip: text }}>{text}</Typography.Text>
        )}
      />

      {/* Authors */}
      <Table.Column
        title="Authors"
        dataIndex="authors"
        key="authors"
        filterIcon={(filtered) => (
          <SearchOutlined
            style={{ color: filtered ? token.colorPrimary : undefined }}
          />
        )}
        defaultFilteredValue={getDefaultFilter("authors", filters, "contains")}
        filterDropdown={(props) => (
          <FilterDropdown {...props}>
            <Input placeholder="Search authors..." />
          </FilterDropdown>
        )}
        render={(authors: string[]) => authors?.join(", ")}
      />

      {/* Price */}
      <Table.Column
        title="Price"
        dataIndex="price"
        key="price"
        align="right"
        sorter
        defaultSortOrder={getDefaultSortOrder("price", sorters)}
        render={(value: number) => (
          <NumberField
            value={value}
            style={{ width: "100px", whiteSpace: "nowrap" }}
            options={{ style: "currency", currency: "USD" }}
          />
        )}
      />

      {/* Condition with colored badge */}
      <Table.Column
        title="Condition"
        dataIndex="condition"
        key="condition"
        render={(condition: string) => {
          let color: string;
          switch (condition) {
            case "NEW":
              color = "green";
              break;
            case "LIKE NEW":
              color = "blue";
              break;
            case "GOOD":
              color = "cyan";
              break;
            case "ACCEPTABLE":
              color = "orange";
              break;
            default:
              color = "default";
          }
          return <Tag color={color}>{condition}</Tag>;
        }}
      />

      {/* Stock */}
      <Table.Column
        title="Stock"
        dataIndex="stock"
        key="stock"
        align="right"
        sorter
        defaultSortOrder={getDefaultSortOrder("stock", sorters)}
      />

      {/* Average Rating */}
      <Table.Column
        title="Avg. Rating"
        dataIndex="averageRating"
        key="averageRating"
        align="right"
        sorter
        defaultSortOrder={getDefaultSortOrder("averageRating", sorters)}
        render={(v: number) => v.toFixed(1)}
      />

      {/* Categories */}
      <Table.Column<IBook>
        title="Categories"
        dataIndex="categoryNames"
        key="categoryIds"
        width={200}
        defaultFilteredValue={getDefaultFilter("categoryIds", filters, "in")}
        filterDropdown={(props) => (
          <FilterDropdown {...props}>
            <Select
              {...categorySelectProps}
              style={{ width: 240 }}
              mode="multiple"
              allowClear
              placeholder="Filter categories"
            />
          </FilterDropdown>
        )}
        render={(_, record) => record.categoryNames.join(", ")}
      />

      {/* Actions */}
      <Table.Column
        title="Actions"
        key="actions"
        fixed="right"
        align="center"
        render={(_, record: IBook) => (
          <Button
            icon={<EyeOutlined />}
            onClick={() =>
              go({
                to: `${showUrl("books", record.id)}`,
                query: { to: pathname },
                options: { keepQuery: true },
                type: "replace",
              })
            }
          />
        )}
      />
    </Table>
  );
};
