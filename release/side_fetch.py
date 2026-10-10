def fetch_all_pages(get_json, path, size):
    first = get_json(path, {"page": 0, "size": size})
    collected = dict(first)
    collected.pop("page", None)
    collected.pop("size", None)
    items = list(first["items"])
    page = 0
    while first["items"] and len(items) < first["totalCount"]:
        page += 1
        following = get_json(path, {"page": page, "size": size})
        if not following["items"]:
            break
        items.extend(following["items"])
    collected["items"] = items
    return collected
