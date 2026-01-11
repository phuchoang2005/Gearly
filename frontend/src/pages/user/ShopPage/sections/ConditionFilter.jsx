import React from 'react'
import ConditionTag from '@u_components/products/ConditionTag'

export default function ConditionFilter({condition, setCondition, showAll, toggleShowAll}) {
    const options = showAll ? ['NEW', 'LIKE NEW', 'GOOD', 'ACCEPTABLE'] : ['NEW', 'LIKE NEW']
    return (
        <div className="mb-4">
            <h3 className="font-semibold mb-2 text-sm">Condition</h3>
            <div className="space-y-1">
                {options.map(value => (
                    <label key={value} className="flex items-center space-x-2 cursor-pointer mb-2">
                        <input
                            type="radio"
                            name="condition"
                            value={value}
                            checked={condition === value}
                            onChange={() => setCondition(condition === value ? '' : value)}
                            className="w-3.5 h-3.5"
                        />
                        <ConditionTag type={value} isFilter/>
                    </label>
                ))}
                <button onClick={toggleShowAll} className="text-xs text-[#1C387F] font-medium hover:underline mt-1">
                    {showAll ? 'See less' : 'See more'}
                </button>
            </div>
        </div>
    )
}