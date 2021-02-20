# shellcheck shell=sh
# shellcheck disable=SC2039
{

if [ -z "$RELOAD" ] && [ -n "$X_BASH_SRC_PATH" ]; then
    return 0 2>/dev/null || exit 0
fi

if curl --version 1>/dev/null 2>&1; then
    x_http_get(){
        curl --fail "${1:?Provide target URL}"; 
        local code=$?
        [ $code -eq 28 ] && return 4
        return $code
    }
else
    x author | grep "Edwin.JH.Lee & LTeam" 1>/dev/null 2>/dev/null || x_activate
    x_http_get(){
        x cat "${1:?Provide target URL}"
    }
fi

X_BASH_SRC_SHELL="sh"
if [ -n "$ZSH_VERSION" ]; then      X_BASH_SRC_SHELL="zsh"
elif [ -n "$BASH_VERSION" ]; then   X_BASH_SRC_SHELL="bash"
fi
export X_BASH_SRC_SHELL

# It is NOT set in some cases.
TMPDIR=${TMPDIR:-$(dirname "$(mktemp -u)")/}
export TMPDIR

echo "Start initializing." >&2

X_BASH_SRC_PATH="$HOME/.x-cmd.com/x-bash"
# TODO: What if zsh
if [ $X_BASH_SRC_SHELL = bash ]; then
    # BUG Notice, if we use eval instead of source to introduce the code, the BASH_SOURCE[0] will not be the location of this file.
    if grep "xrc()" "${BASH_SOURCE[0]}" 1>/dev/null 2>&1; then
        X_BASH_SRC_PATH=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
    else
        echo "Script is NOT executed by source. So we have to guess $X_BASH_SRC_PATH as its path" >&2
    fi
fi

boot_debug "Setting env X_BASH_SRC_PATH: $X_BASH_SRC_PATH"

mkdir -p "$X_BASH_SRC_PATH"

STR_REGEX_SEP="$(printf "\001")"
str_regex(){
    # Only dash does not support pattern="${pattern//\\/\\\\}"
    awk -v FS="${STR_REGEX_SEP}" '{
        if (match($1, $2))  exit 0
        else                exit 1
    }' <<A
${1}${STR_REGEX_SEP}${2:?str_regex(): Provide pattern}
A
}

# https://sh.x-cmd.com
cat >"$X_BASH_SRC_PATH/.source.mirror.list" <<A
https://x-bash.github.io
https://x-bash.gitee.io
A

echo "Creating $X_BASH_SRC_PATH/.source.mirror.list" >&2

# shellcheck disable=SC2120
xrc_mirrors(){
    local fp="$X_BASH_SRC_PATH/.source.mirror.list"
    if [ $# -ne 0 ]; then
        local IFS=$'\n'
        echo "$*" >"$fp"
    else
        cat "$fp"
    fi
    return 0
}

xrc_clear(){
    if [ -f "${X_BASH_SRC_PATH:?Env X_BASH_SRC_PATH should not be empty.}/boot" ]; then
        if [ "$X_BASH_SRC_PATH" = "/" ]; then
            echo "Env X_BASH_SRC_PATH should not be /" >&2
        else
            rm -rf "$X_BASH_SRC_PATH";
        fi
    else
        echo "'$X_BASH_SRC_PATH/boot' NOT found." >&2
    fi
}

xrc_cache(){ echo "$X_BASH_SRC_PATH"; }
x_activate(){
    X_BASH_X_CMD_PATH="$(command -v x)"
    x(){
        case "$1" in
            rc|src) SRC_LOADER=bash eval "$(_xrc_print_code "$@")" ;;
            # java | jar);;
            # python | py);;
            # javascript | js);;
            # typescript | ts);;
            # ruby | rb);;
            # lua);;
            *) "$X_BASH_X_CMD_PATH" "$@" ;;
        esac
    }
}

xrc(){
    if [ $# -eq 0 ]; then
        cat >&2 <<A
xrc     x-bash core function.
        Uasge:  xrc <lib> [<lib>...]
        Notice, builtin command 'source' format is 'source <lib> [argument...]'"
A
        return 1
    fi
    
    for i in "$@"; do
        eval "$(_xrc_print_code "$i")" || return
    done
    return 0
}

# shellcheck disable=SC2046
xrc_cat(){      cat $(xrc_which "$@");      }

xrc_curl(){
    local REDIRECT=/dev/stdout
    if [ -n "$CACHE" ]; then
        if [ -z "$UPDATE" ] && [ -f "$CACHE" ]; then
            xrc_debug "xrc_curl() terminated. Because update is NOT forced and file existed: $CACHE"
            return 0
        fi
        REDIRECT=$TMPDIR.x-bash-temp-download.$RANDOM
    fi

    x_http_get "$1" 1>"$REDIRECT" 2>/dev/null
    local code=$?
    xrc_debug "x_http_get $1 return code: $code"
    if [ $code -eq 0 ]; then 
        if [ -n "$CACHE" ]; then
            xrc_debug "Copy the temp file to CACHE file: $CACHE"
            mkdir -p "$(dirname "$CACHE")"
            mv "$REDIRECT" "$CACHE"
        fi
    fi
    return $code
}

xrc_curl_gitx(){   # Simple strategy
    local IFS i=1 mirror mod="${1:?Provide location like std/str}"
    local mirror_list
    mirror_list="$(xrc_mirrors)"
    for mirror in $mirror_list; do
        xrc_debug "Trying xrc_curl $mirror/$mod"
        xrc_curl "$mirror/$mod"
        case $? in
        0)  if [ "$i" -ne 1 ]; then
                xrc_debug "First guess NOW is $mirror"
                xrc_mirrors "$mirror
$(echo "$mirror_list" | awk "NR!=$i{ print \$0 }" )"
            fi
            return 0;;
        4)  return 4;;
        esac
        i=$((i+1))  # Support both ash, dash, bash
    done
    return 1
}

xrc_which(){
    if [ $# -eq 0 ]; then
        cat >&2 <<A
xrc_which  Download lib files and print the local path.
        Uasge:  xrc_which <lib> [<lib>...]
        Example: source "$(xrc_which std/str)"
A
        return 1
    fi
    local i
    for i in "$@"; do
        _xrc_which_one "$i" || return
    done
}

_xrc_which_one(){
    local RESOURCE_NAME=${1:?Provide resource name};

    local handler

    if [ "${RESOURCE_NAME#/}" != "${RESOURCE_NAME}" ]; then
        echo "$RESOURCE_NAME"; return 0
    fi

    if str_regex "$RESOURCE_NAME" "^\.\.?/"; then
        local tmp
        if tmp="$(cd "$(dirname "$RESOURCE_NAME")" || exit 1; pwd)"; then
            echo "$tmp/$(basename "$RESOURCE_NAME")"
            return 0
        else
            echo "local file not exists: $RESOURCE_NAME" >&2
            return 1
        fi
    fi

    local TGT
    if str_regex "$RESOURCE_NAME" "^https?://" ; then
        # that relies on base64?
        TGT="$X_BASH_SRC_PATH/BASE64-URL-$(echo -n "$RESOURCE_NAME" | base64 | tr -d '\r\n')"
        if ! CACHE="$TGT" xrc_curl "$RESOURCE_NAME"; then
            echo "ERROR: Fail to load http resource due to network error or other: $RESOURCE_NAME " >&2
            return 1
        fi

        echo "$TGT"
        return 0
    fi

    local module="$RESOURCE_NAME"

    # If it is short alias like str (short for str/latest)
    if ! str_regex "$module" "/" ; then
        module="$module/latest"
    fi
    TGT="$X_BASH_SRC_PATH/$module"

    if ! CACHE="$TGT" xrc_curl_gitx "$module"; then
        echo "ERROR: Fail to load $RESOURCE_NAME due to network error or other. Do you want to load std/$RESOURCE_NAME?" >&2
        return 1
    fi

    echo "$TGT"
}

_xrc_print_code(){
    local TGT 
    local RESOURCE_NAME=${1:?Provide resource name}; shift

    if ! TGT="$(_xrc_which_one "$RESOURCE_NAME")"; then
        echo "Aborted. Because '_xrc_which_one $RESOURCE_NAME' fails" >&2
        return 1
    fi

    echo "${SRC_LOADER:-.}" "$TGT" "$@"
}

alias xrcw=xrc_which
alias xrcc=xrc_cat

}
