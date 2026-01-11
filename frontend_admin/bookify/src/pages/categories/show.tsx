
// src/pages/categories.tsx
import React from 'react';
import {
  IResourceComponentsProps,
  useTable,
  useForm,
  useShow,
} from '@refinedev/core';
import {
  List,
  DateField,
  Create,
  Edit,
  Show,
  EditButton,
  ShowButton,
  DeleteButton,
} from '@refinedev/antd';
import {    Table,
  Form,
  Input,
} from 'antd';
import { Typography } from 'antd';

const { Title, Text } = Typography;


export const CategoryShow: React.FC<IResourceComponentsProps> = () => {
  const { queryResult } = useShow();
  const record = queryResult?.data?.data;

  return (
      <Show title="Category Details">
        <Title level={5}>ID</Title>
        <Text>{record?.id}</Text>
        <Title level={5}>Name</Title>
        <Text>{record?.name}</Text>
      </Show>
  );
};
