/** Parses an :empNo route parameter; anything that is not a positive integer is null. */
export function parseEmpNo(value: string | undefined): number | null {
  if (!value || !/^\d+$/.test(value)) {
    return null;
  }
  const empNo = Number(value);
  return empNo > 0 ? empNo : null;
}
