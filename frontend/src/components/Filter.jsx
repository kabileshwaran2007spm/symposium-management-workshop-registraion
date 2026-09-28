import Select from './Select';
import { categoryLabel } from '../utils/format';

const CATEGORIES = ['WORKSHOP', 'SYMPOSIUM', 'SEMINAR', 'HACKATHON', 'TECHNICAL', 'CULTURAL'];
const DEPARTMENTS = ['CSE', 'IT', 'ECE', 'EEE', 'MECH', 'CIVIL', 'AIML', 'ENG', 'MGMT'];

const AVAILABILITY = [
  { value: 'AVAILABLE', label: 'Seats available' },
  { value: 'ALMOST_FULL', label: 'Almost full' },
  { value: 'FULL', label: 'Full' },
];

function toOptions(values) {
  return values.map((value) => ({ value, label: categoryLabel(value) }));
}

/**
 * Filter bar for event discovery.
 *
 * Each control is optional; an empty value simply removes that constraint from the
 * query, so filters compose without any "apply" step.
 */
export default function Filter({ values, onChange }) {
  const set = (field) => (event) => onChange({ ...values, [field]: event.target.value });

  return (
    <div className="filter-bar">
      <Select
        label="Category"
        placeholder="All categories"
        options={toOptions(CATEGORIES)}
        value={values.category}
        onChange={set('category')}
      />

      <Select
        label="Department"
        placeholder="All departments"
        options={DEPARTMENTS.map((d) => ({ value: d, label: d }))}
        value={values.department}
        onChange={set('department')}
      />

      <Select
        label="Availability"
        placeholder="Any availability"
        options={AVAILABILITY}
        value={values.availability}
        onChange={set('availability')}
      />

      <div className="filter-field">
        <label className="filter-label" htmlFor="filter-date">
          Date
        </label>
        <input
          id="filter-date"
          className="input"
          type="date"
          value={values.date}
          onChange={set('date')}
        />
      </div>
    </div>
  );
}
