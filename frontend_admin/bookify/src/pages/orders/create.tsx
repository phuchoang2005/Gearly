import React, { useEffect } from "react";
import { Create, useForm, useSelect } from "@refinedev/antd";
import { HttpError } from "@refinedev/core";
import { Form, Select, InputNumber, Space, Button, Input } from "antd";
import { PlusOutlined, MinusCircleOutlined } from "@ant-design/icons";
import type { IUser, IBook, IOrder } from "../../../interfaces";

export const OrderCreate: React.FC = () => {
  const { formProps, saveButtonProps } = useForm<IOrder, HttpError>({
    resource: "orders",
    action: "create",
    initialValues: {
      orderStatus: "PENDING",
      items: [],
      totalAmount: 0,
      shippingInformation: {
        firstName: "",
        lastName: "",
        email: "",
        phoneNumber: "",
        address: {
          street: "",
          city: "",
          state: "",
          postalCode: "",
          country: "",
        },
      },
      payment: { method: "cod" },
    },
  });

  // fetch users
  const {
    selectProps: userSelectProps,
    queryResult: { data: usersData },
  } = useSelect<IUser>({
    resource: "users",
    optionLabel: "fullName",
    optionValue: "id",
    pagination: { pageSize: 1000 },
  });
  const users = usersData?.data || [];

  // fetch books
  const {
    selectProps: bookSelectProps,
    queryResult: { data: booksData },
  } = useSelect<IBook>({
    resource: "books",
    optionLabel: "title",
    optionValue: "id",
    pagination: { pageSize: 20 },
    onSearch: (value: string) => [
      { field: "title", operator: "contains", value },
    ],
  });
  const books = booksData?.data || [];

  // watch userId
  const selectedUserId = Form.useWatch("userId", formProps.form);

  // whenever userId changes, fill shippingInformation
  useEffect(() => {
    if (selectedUserId) {
      const u = users.find((x) => x.id === selectedUserId);
      if (u) {
        formProps.form.setFieldsValue({
          shippingInformation: {
            firstName: u.firstName,
            lastName: u.lastName,
            email: u.email,
            phoneNumber: u.phone,
            address: {
              street: u.address.street,
              city: u.address.city,
              state: u.address.state,
              postalCode: u.address.postalCode,
              country: u.address.country,
            },
          },
        });
      }
    }
  }, [selectedUserId, users]);

  // watch items to auto-fill title/price/qty and recalc totalAmount
  const items: Array<{
    bookId?: string;
    title?: string;
    price?: number;
    quantity?: number;
  }> = Form.useWatch("items", formProps.form) || [];

  useEffect(() => {
    let total = 0;
    console.log("items: ", items);

    items.forEach((item: any, idx: number) => {
      // ← guard against undefined or empty row
      if (!item || !item.bookId) {
        return;
      }
      if (item.price && item.quantity) {
        console.log("item inside: ", item);
        total += item.price * item.quantity;
      }
      // lookup the book
      const b = books.find((x) => x.id === item.bookId);
      if (!b) {
        return;
      }
      if (!("price" in item))
        // patch in title/price/qty (if fresh)
        formProps.form.setFields([
          { name: ["items", idx, "title"], value: b.title },
          { name: ["items", idx, "price"], value: b.price },
          { name: ["items", idx, "quantity"], value: 1 },
        ]);

      // accumulate
    });
    formProps.form.setFieldsValue({ totalAmount: total });
  }, [items, books]);

  return (
    <Create saveButtonProps={saveButtonProps}>
      <Form {...formProps} layout="vertical">
        {/* User */}
        <Form.Item label="User" name="userId" rules={[{ required: true }]}>
          <Select {...userSelectProps} />
        </Form.Item>

        {/* Order Status */}
        {/*<Form.Item*/}
        {/*  label="Status"*/}
        {/*  name="orderStatus"*/}
        {/*  rules={[{ required: true }]}*/}
        {/*>*/}
        {/*  <Select>*/}
        {/*    {[*/}
        {/*      "PENDING",*/}
        {/*      "PROCESSING",*/}
        {/*      "SHIPPED",*/}
        {/*      "DELIVERED",*/}
        {/*      "COMPLETED",*/}
        {/*      "CANCELLED",*/}
        {/*      "REFUNDED",*/}
        {/*    ].map((s) => (*/}
        {/*      <Select.Option key={s} value={s}>*/}
        {/*        {s}*/}
        {/*      </Select.Option>*/}
        {/*    ))}*/}
        {/*  </Select>*/}
        {/*</Form.Item>*/}

        {/* Items */}
        <Form.List name="items">
          {(fields, { add, remove }) => (
            <>
              {fields.map((field) => (
                <Space
                  key={field.key}
                  align="baseline"
                  style={{ marginBottom: 8 }}
                >
                  {/* Book */}
                  <Form.Item
                    {...field}
                    name={[field.name, "bookId"]}
                    fieldKey={[field.fieldKey, "bookId"]}
                    rules={[{ required: true }]}
                  >
                    <Select
                      {...bookSelectProps}
                      showSearch
                      filterOption={false}
                      placeholder="Search book..."
                      style={{ width: 200 }}
                    />
                  </Form.Item>

                  {/* Title */}
                  <Form.Item
                    {...field}
                    name={[field.name, "title"]}
                    fieldKey={[field.fieldKey, "title"]}
                    rules={[{ required: true }]}
                  >
                    <Input placeholder="Title" style={{ width: 200 }} />
                  </Form.Item>

                  {/* Price */}
                  <Form.Item
                    {...field}
                    name={[field.name, "price"]}
                    fieldKey={[field.fieldKey, "price"]}
                    rules={[{ required: true }]}
                    defaultValue={0}
                  >
                    <InputNumber
                      prefix="$"
                      min={0}
                      placeholder="Price"
                      style={{ width: 120 }}
                    />
                  </Form.Item>

                  {/* Quantity */}
                  <Form.Item
                    {...field}
                    name={[field.name, "quantity"]}
                    fieldKey={[field.fieldKey, "quantity"]}
                    rules={[{ required: true }]}
                  >
                    <InputNumber
                      min={1}
                      placeholder="Qty"
                      style={{ width: 80 }}
                    />
                  </Form.Item>

                  <MinusCircleOutlined onClick={() => remove(field.name)} />
                </Space>
              ))}

              <Form.Item>
                <Button
                  type="dashed"
                  onClick={() => {
                    add();
                    bookSelectProps.onSearch("");
                  }}
                  block
                  icon={<PlusOutlined />}
                >
                  Add Item
                </Button>
              </Form.Item>
            </>
          )}
        </Form.List>

        {/* Total Amount */}
        <Form.Item
          label="Total Amount"
          name="totalAmount"
          rules={[{ required: true }]}
        >
          <InputNumber prefix="$" style={{ width: "100%" }} min={0} readOnly />
        </Form.Item>

        {/* Payment Method */}


        {/* Shipping Information */}
        <Form.Item
          label="First Name"
          name={["shippingInformation", "firstName"]}
          rules={[{ required: true }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          label="Last Name"
          name={["shippingInformation", "lastName"]}
          rules={[{ required: true }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          label="Email"
          name={["shippingInformation", "email"]}
          rules={[{ required: true, type: "email" }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          label="Phone Number"
          name={["shippingInformation", "phoneNumber"]}
          rules={[{ required: true }]}
        >
          <Input />
        </Form.Item>

        {/* Address */}
        <Form.Item
          label="Street"
          name={["shippingInformation", "address", "street"]}
          rules={[{ required: true }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          label="City"
          name={["shippingInformation", "address", "city"]}
          rules={[{ required: true }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          label="State"
          name={["shippingInformation", "address", "state"]}
          rules={[{ required: true }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          label="Postal Code"
          name={["shippingInformation", "address", "postalCode"]}
          rules={[{ required: true }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          label="Country"
          name={["shippingInformation", "address", "country"]}
          rules={[{ required: true }]}
        >
          <Input />
        </Form.Item>
      </Form>
    </Create>
  );
};
