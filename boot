#! /usr/bin/env bash

[ -z "$__X_CMD_COM_BOOT_IMPORTED" -o ! -z "$RELOAD" ] && {

    echo "Initialize the boot enviroment"
    __X_CMD_COM_BOOT_IMPORTED=true

    # TODO: Get rid of this function
    @init.curl(){
        if [ ! "$CURL" == "" ]; then
            return
        fi

        curl --version 1>/dev/null 2>&1
        if [ $? -eq 0 ]; then
            export CURL=curl
            return
        fi

        which x 1>/dev/null 2>&1
        if [ $? -eq 0 ]; then
            export CURL="x cat"
            return
        fi
        
        return 1
    }

    @src.one(){
        @init.curl

        if [[ "$1" =~ ^http:// ]] || [[ "$1" =~ ^https:// ]]; then
            local URL="$1"
            local TGT="$HOME/.x-cmd.com/x-bash/$(echo $URL | base64)"
        else
            local module=${1:?provide module name}
            if [[ ! $module =~ \/ ]]; then
                local index_file="$HOME/.x-cmd.com/x-bash/index"
                # File not exists or file is not modified more than one hour
                if [ ! -r $index_file ] || [[ $(find "$index_file" -mtime +1h -print) ]]; then
                    mkdir -p $(dirname "$index_file")
                    local content="$($CURL "https://x-bash.github.io/index" 2>/dev/null)"
                    (echo "$content" | grep "std/str" 1>/dev/null) && echo "$content" >$index_file
                fi

                module="$(grep "$1" "$index_file" | head -n 1)"
                echo "Try using $module" >&2
            fi

            local URL="https://x-bash.github.io/$module"
            local TGT="$HOME/.x-cmd.com/x-bash/$module"
        fi

        if [ ! -e "$TGT" ]; then
            mkdir -p $(dirname "$TGT")

            $CURL "$URL" >"$TGT" 2>/dev/null
            if grep ^\<\!DOCTYPE "$TGT" >/dev/null; then
                rm "$TGT"
                echo "Failed to load $1, do you want to load std/$1?"
                return 1
            fi
        fi
        
        [ $? -eq 0 ] && ${X_CMD_COM_PARAM_CMD:-source} "$TGT"
    }

    @src(){ for i in "$@"; do @src.one $1; done }

    @src.clear-cache(){
        rm -rf "$HOME/.x-cmd.com/x-bash"
    }

    @run(){
        X_CMD_COM_PARAM_CMD=bash @src "$*"
    }

}
