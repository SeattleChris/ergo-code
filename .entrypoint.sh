#!/bin/bash

# Move this file to /web directory?
# change PROJECT to correct project name
# chmod to allow this to run the script

set -e

cd /src

python manage.py makemigrations --noinput
python manage.py migrate --noinput
python manage.py collectstatic --noinput

#python manage.py runserver 0.0.0.0:8000
gunicorn PROJECT.wsgi:application -w 3 -b :8000
