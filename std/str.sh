#! /usr/bin/env bash

# https://stackoverflow.com/questions/1527049/how-can-i-join-elements-of-an-array-in-bash
# join_ws also works

str.join(){
    local sep=$1
    shift 1
    local bar=$(printf "${sep}%s" "$@")
    bar=${bar:${#sep}}
    echo $bar
}

str.trim(){
    local var="$*"
    # remove leading whitespace characters
    var="${var#"${var%%[![:space:]]*}"}"
    # remove trailing whitespace characters
    var="${var%"${var##*[![:space:]]}"}"   
    echo -n "$var"
}

str.trim_left(){
    :
}

str.trim_right(){
    :
}

# According to 
str.split(){
    echo ${1:?source string} | tr ${2:?split char} '\n'
}

str.upper(){ echo -n $1 | tr [:lower:] [:upper:]; }
str.lower(){ echo -n $1 | tr [:upper:] [:lower:]; }

# other format using library

## text

str.dos2unix(){
    if [ $# -eq 0 ]; then
        sed -e 's/\r//'
    else
        sed -e 's/\r//' -i ${BAK:-""} "$@"
    fi
}

# refer https://en.wikipedia.org/wiki/Unix2dos
# refer https://www.cyberciti.biz/faq/howto-unix-linux-convert-dos-newlines-cr-lf-unix-text-format/
str.unix2dos(){
    if [ $# -eq 0 ]; then
        # test cat abc.txt | sed -e 's/$/\r/' | cat -vet -
        sed -e $'s/$/\r/'
    else
        # test cat abc.txt | sed -e 's/$/\r/' | cat -vet -
        # cat abc.txt | sed -e 's/$/\r/' | cat -vet -
        sed -e 's/$/\r/' -e "$ s/..$//g" -i ${BAK:-""} "$@"
        # sed -e 's/\r*$/\r/' -i ${BAK:-""} "$@"
        # sed -e "s/$/^M/" -i ${BAK:-""} "$@"
    fi
}

# USAGE 1, remove in file: remove_eol_space filepath
# USAGE 2, remove in file and backup with BAK as extensions: BAK='.bak' remove_eol_space filepath
# USAGE 3, remove and output to stdout: cat filepath | remove_eol_space
str.remove_eol_space(){
    if [ $# -eq 0 ]; then
        sed -e 's/[[:blank:]]*$//g'
    else
        sed -e 's/[[:blank:]]*$//g' -i ${BAK:-""} "$@"
    fi
}


## Regular Expression design

OR="\|"

R.wrap(){
    echo -n "\($1\)"
}

R.or(){
    R.wrap $(str.join "\|" "$@")
}

# IP_0_255="[0-9]${OR}\([1-9][0-9]\)${OR}\(1[0-9][0-9]\)${OR}\(2[0-4][0-9]\)${OR}\(25[0-5]\)"
IP_0_255=$( R.or `R.wrap [0-9]` `R.wrap [1-9][0-9]` `R.wrap 1[0-9][0-9]` `R.wrap 2[0-4][0-9]` `R.wrap 25[0-5]` )
IP="\\b${IP_0_255}\.${IP_0_255}\.${IP_0_255}\.${IP_0_255}\\b"




