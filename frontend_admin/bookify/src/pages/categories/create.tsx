
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


export const CategoryCreate: React.FC<IResourceComponentsProps> = () => {
    const { formProps, saveButtonProps } = useForm();
    return (
        <Create saveButtonProps={saveButtonProps} title="Create Category">
            <Form {...formProps} layout="vertical">
                <Form.Item
                    label="Name"
                    name="name"
                    rules={[{ required: true, message: 'Please enter category name' }]}
                >
                    <Input />
                </Form.Item>
            </Form>
        </Create>
    );
};
