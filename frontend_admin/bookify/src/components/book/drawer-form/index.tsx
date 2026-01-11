import React from "react";
import { SaveButton, useDrawerForm } from "@refinedev/antd";
import {
  BaseKey,
  useApiUrl,
  useGetToPath,
  useGo,
  useTranslate,
} from "@refinedev/core";
import { getValueFromEvent, useSelect } from "@refinedev/antd";
import {
  Form,
  Input,
  InputNumber,
  Select,
  Upload,
  Grid,
  Button,
  Flex,
  Avatar,
  Segmented,
  Spin,
} from "antd";
import { UploadOutlined } from "@ant-design/icons";
import { useSearchParams } from "react-router";
import { Drawer } from "../../drawer";
import type { IBook, ICategory } from "../../../interfaces";
import { useStyles } from "./styled";

type Props = {
  id?: BaseKey;
  action: "create" | "edit";
  onClose?: () => void;
  onMutationSuccess?: () => void;
};

export const BookDrawerForm: React.FC<Props> = (props) => {
  const getToPath = useGetToPath();
  const [searchParams] = useSearchParams();
  const go = useGo();
  const t = useTranslate();
  const apiUrl = useApiUrl();
  const breakpoint = Grid.useBreakpoint();
  const { styles, theme } = useStyles();

  const {
    drawerProps,
    formProps,
    close,
    saveButtonProps,
    onFinish: refineOnFinish,
    formLoading,
  } = useDrawerForm<IBook>({
    resource: "books",
    id: props?.id,
    action: props.action,
    redirect: false,
    onMutationSuccess: () => props.onMutationSuccess?.(),

    // **Transform** the images field before submitting
    transform: (values) => {
      // values.images is the AntD file-list
      const images = (values.images || []).map((file: any) => ({
        url: file.response?.url,
        alt: file.name,
      }));
      return {
        ...values,
        images,
      };
    },
  });

  console.log("BookDrawerForm props:", props);
  console.log("BookDrawerForm formprops:", formProps);

  const { selectProps: categorySelectProps } = useSelect<ICategory>({
    resource: "categories",
    optionLabel: "name",
    optionValue: "id",
  });

  const onDrawerClose = () => {
    close();
    if (props.onClose) {
      props.onClose();
      return;
    }
    go({
      to: searchParams.get("to") ?? getToPath({ action: "list" }) ?? "",
      query: { to: undefined },
      options: { keepQuery: true },
      type: "replace",
    });
  };
  // wrap the save button so it triggers our form’s submit
  const wrappedSaveProps = {
    ...saveButtonProps,
    onClick: () => formProps.form.submit(),
  };
  const images = Form.useWatch("images", formProps.form);
  const image = images?.[0] ?? null;
  const previewImageURL = image?.url || image?.response?.url;
  const title =
    props.action === "edit" ? undefined : t("books.actions.add") ?? "Add Book";

  return (
    <Drawer
      {...drawerProps}
      open={true}
      title={title}
      width={breakpoint.sm ? 378 : "100%"}
      zIndex={1001}
      onClose={onDrawerClose}
    >
      <Spin spinning={formLoading}>
        {/*<Form {...formProps} layout="vertical">*/}
        <Form
          {...formProps}
          layout="vertical"
          onFinish={(values) => {
            const images = (values.images || []).map((file: any) => ({
              url: file.response?.url,
              alt: file.name,
            }));

            refineOnFinish?.({
              ...values,
              images,
            });
          }}
        >
          {/* Image upload */}
          <Form.Item
            name="images"
            valuePropName="fileList"
            getValueFromEvent={getValueFromEvent}
            rules={[{ required: true, message: "Please upload a cover image" }]}
            style={{ marginBottom: 16 }}
          >
            <Upload.Dragger
              name="file"
              action={`${apiUrl}/media/upload`}
              maxCount={1}
              accept=".png,.jpg,.jpeg"
              className={styles.uploadDragger}
              showUploadList={false}
            >
              <Flex
                vertical
                align="center"
                justify="center"
                style={{ height: 200 }}
              >
                <Avatar
                  shape="square"
                  style={{
                    aspectRatio: 1,
                    objectFit: "contain",
                    width: previewImageURL ? "100%" : 48,
                    height: previewImageURL ? "100%" : 48,
                  }}
                  src={previewImageURL || "/images/book-default.png"}
                />
                <Button icon={<UploadOutlined />} style={{ marginTop: 16 }}>
                  {t("books.fields.images.placeholder") ?? "Upload cover"}
                </Button>
              </Flex>
            </Upload.Dragger>
          </Form.Item>

          {/* Title */}
          <Form.Item
            label={t("books.fields.title")}
            name="title"
            rules={[{ required: true }]}
          >
            <Input />
          </Form.Item>

          {/* Authors */}
          <Form.Item
            label={t("books.fields.authors")}
            name="authors"
            rules={[
              { required: true, message: "Please add at least one author" },
            ]}
          >
            <Select mode="tags" placeholder="Add authors" />
          </Form.Item>

          {/* Description */}
          <Form.Item
            label={t("books.fields.description")}
            name="description"
            rules={[{ required: true }]}
          >
            <Input.TextArea rows={4} />
          </Form.Item>

          {/* Prices */}
          <Flex style={{ gap: 16 }}>
            <Form.Item
              label={t("books.fields.price")}
              name="price"
              rules={[{ required: true }]}
            >
              <InputNumber prefix="$" style={{ width: "100%" }} min={0} />
            </Form.Item>
            <Form.Item
              label={t("books.fields.originalPrice")}
              name="originalPrice"
              rules={[{ required: true }]}
            >
              <InputNumber prefix="$" style={{ width: "100%" }} min={0} />
            </Form.Item>
          </Flex>

          {/* Condition & Stock */}
          <Flex style={{ gap: 16 }}>
            <Form.Item label={t("books.fields.condition")} name="condition">
              {/*<Segmented*/}
              {/*    options={[*/}
              {/*        { label: "NEW", value: "NEW" },*/}
              {/*        { label: "LIKE NEW", value: "LIKENEW" },*/}
              {/*        { label: "GOOD", value: "GOOD" },*/}
              {/*        { label: "ACCEPTABLE", value: "ACCEPTABLE" },*/}
              {/*    ]}*/}
              {/*/>*/}
              <Segmented
                options={[
                  {
                    label: t("books.fields.condition.options.new", "NEW"),
                    value: "NEW",
                  },
                  {
                    label: t(
                      "books.fields.condition.options.likeNew",
                      "LIKE NEW"
                    ),
                    value: "LIKE NEW",
                  },
                  {
                    label: t("books.fields.condition.options.good", "GOOD"),
                    value: "GOOD",
                  },
                  {
                    label: t(
                      "books.fields.condition.options.acceptable",
                      "ACCEPTABLE"
                    ),
                    value: "ACCEPTABLE",
                  },
                ]}
              />
            </Form.Item>
          </Flex>
          <Form.Item
            label={t("books.fields.stock")}
            name="stock"
            rules={[{ required: true }]}
          >
            <InputNumber style={{ width: "100%" }} min={0} />
          </Form.Item>

          {/* Categories */}
          <Form.Item
            label={t("books.fields.categories")}
            name="categoryIds"
            rules={[
              { required: true, message: "Select one or more categories" },
            ]}
          >
            <Select
              {...categorySelectProps}
              placeholder="Select categories"
              mode={"multiple"}
            />
          </Form.Item>

          {/* Save / Cancel */}
          <Flex justify="end" style={{ marginTop: 24 }}>
            <Button onClick={onDrawerClose} style={{ marginRight: 8 }}>
              {t("actions.cancel")}
            </Button>
            <SaveButton {...wrappedSaveProps} type="primary">
              {t("actions.save")}
            </SaveButton>
          </Flex>
        </Form>
      </Spin>
    </Drawer>
  );
};
