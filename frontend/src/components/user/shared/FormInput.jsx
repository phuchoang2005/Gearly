import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

export default function FormInput({ label, error, required, type = 'text', placeholder, value, ...rest }) {
    const [show, setShow] = useState(false);
    const id = rest.name;
    const inputType = type === 'password' && show ? 'text' : type;

    return (
        <div className="w-full">
            <label htmlFor={id} className="block text-sm font-medium text-gray-900 mb-1">
                {label}
                {required && <span className="text-red-600">*</span>}
            </label>

            <div className="relative">
                <input
                    id={id}
                    type={inputType}
                    placeholder={placeholder}
                    value={value}
                    {...rest}
                    className={`w-full border border-gray-300 text-sm rounded px-3 py-2
                    placeholder-gray-400
                    focus:outline-none focus:ring-1 focus:ring-black focus:border-black bg-white
                    ${error ? 'border-red-500 focus:ring-red-500' : ''}`}
                />
                {type === 'password' && (
                    <button
                        type="button"
                        onClick={() => setShow(!show)}
                        tabIndex={-1}
                        className="absolute inset-y-0 right-3 flex items-center text-gray-400 hover:text-gray-600"
                    >
                        {show ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                )}
            </div>

            {error && <p className="text-sm text-red-500 mt-1">{error}</p>}
        </div>
    );
}
