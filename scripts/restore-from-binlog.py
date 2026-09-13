"""
从 MySQL binlog（ROW 格式）解析出被删除的行数据，生成恢复用的 INSERT 语句。

输入：mysqlbinlog --base64-output=DECODE-ROWS -v 的解码文本
输出：restore.sql（INSERT IGNORE 语句，可重复执行）
"""

import re
import sys
import os

SRC = sys.argv[1] if len(sys.argv) > 1 else r"D:\java\BlogSystem\target\dbcheck\bl943.txt"
OUT = sys.argv[2] if len(sys.argv) > 2 else r"D:\java\BlogSystem\target\dbcheck\restore.sql"

# 表字段顺序（按 SHOW COLUMNS 的真实顺序，与 binlog 的 @1..@N 一一对应）
# 注意：blog_info.published_status 是后加的列，物理顺序在最后
COLS = {
    "user_info": ["id", "user_name", "password", "github_url",
                  "delete_flag", "create_time", "update_time"],
    "blog_info": ["id", "title", "content", "user_id",
                  "delete_flag", "create_time", "update_time", "published_status"],
}

# 匹配 "### DELETE FROM `db`.`table`"
RE_DELETE = re.compile(r"^###\s+DELETE FROM `[^`]+`\.`(\w+)`")
# 匹配 "###   @1=123" / "###   @2='text'" / "###   @4=NULL"
RE_COL = re.compile(r"^###\s+@(\d+)=(.*)$")


def parse(path):
    """解析解码后的 binlog 文本，返回 {表名: [行字典, ...]}"""
    rows = {t: [] for t in COLS}
    cur_table = None
    cur = {}

    def flush():
        if cur_table and cur:
            rows[cur_table].append(dict(cur))

    with open(path, encoding="utf-8", errors="replace") as fp:
        for raw in fp:
            line = raw.rstrip("\n")
            m = RE_DELETE.match(line)
            if m:
                flush()
                cur = {}
                t = m.group(1)
                cur_table = t if t in COLS else None
                continue
            if cur_table:
                m2 = RE_COL.match(line)
                if m2:
                    cur[int(m2.group(1))] = m2.group(2)
                    continue
                # 遇到新的 ### 事件（非本行数据）就结束当前行
                if line.startswith("###") and not line.startswith("### WHERE"):
                    flush()
                    cur = {}
                    cur_table = None
    flush()
    return rows


def build_sql(rows):
    lines = [
        "-- 由 binlog 解析生成的恢复脚本（INSERT IGNORE，可安全重复执行）",
        "USE java_blog_spring;",
        "SET NAMES utf8mb4;",
        "",
    ]
    total = 0
    for table, cols in COLS.items():
        data = rows.get(table, [])
        lines.append(f"-- {table}: {len(data)} 行")
        for row in data:
            values = []
            for i in range(1, len(cols) + 1):
                v = row.get(i)
                values.append("NULL" if v is None else v)
            # 主键缺失则跳过（解析异常保护）
            if values[0] == "NULL":
                continue
            lines.append(
                f"INSERT IGNORE INTO {table} ({', '.join(cols)}) VALUES ({', '.join(values)});"
            )
            total += 1
        lines.append("")
    lines.append(f"-- 共 {total} 条 INSERT")
    return "\n".join(lines), total


if __name__ == "__main__":
    if not os.path.exists(SRC):
        print(f"输入文件不存在: {SRC}")
        sys.exit(1)
    parsed = parse(SRC)
    for t, r in parsed.items():
        print(f"{t}: 解析出 {len(r)} 行")
    sql, total = build_sql(parsed)
    with open(OUT, "w", encoding="utf-8") as fp:
        fp.write(sql)
    print(f"已生成 {OUT}（{total} 条 INSERT）")
