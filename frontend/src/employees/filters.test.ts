import { describe, expect, it } from 'vitest';

import { parseEmpNo } from './empNo';
import { filtersFromParams, filtersToParams, pageFromParams } from './filters';

describe('search filters in the URL', () => {
  it('round-trips filters and drops empty values', () => {
    const params = filtersToParams(
      { q: ' Brid ', deptNo: 'd005', title: '', hiredFrom: '1990-01-01' },
      2,
    );

    expect(params.toString()).toBe('q=Brid&deptNo=d005&hiredFrom=1990-01-01&page=2');
    expect(filtersFromParams(params)).toEqual({
      q: 'Brid',
      deptNo: 'd005',
      hiredFrom: '1990-01-01',
    });
    expect(pageFromParams(params)).toBe(2);
  });

  it('treats a bad page number as the first page', () => {
    expect(pageFromParams(new URLSearchParams('page=-3'))).toBe(0);
    expect(pageFromParams(new URLSearchParams('page=abc'))).toBe(0);
  });
});

describe('parseEmpNo', () => {
  it('accepts positive integers only', () => {
    expect(parseEmpNo('10012')).toBe(10012);
    expect(parseEmpNo('0')).toBeNull();
    expect(parseEmpNo('12a')).toBeNull();
    expect(parseEmpNo(undefined)).toBeNull();
  });
});
