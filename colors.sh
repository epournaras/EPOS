#!/bin/bash

function green
{
	echo -e "\x1B[00;32m$1\x1B[00m" 1>&2;
}


function orange
{
	echo -e "\x1B[00;33m$1\x1B[00m" 1>&2;
}


function red
{
	echo -e "\x1B[00;31m$1\x1B[00m" 1>&2;
}

